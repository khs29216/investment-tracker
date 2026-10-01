package com.project.investment_tracker.service;

import com.project.investment_tracker.controller.TradePerformanceController;
import com.project.investment_tracker.entity.Account;
import com.project.investment_tracker.entity.Trade;
import com.project.investment_tracker.entity.TradeType;
import com.project.investment_tracker.global.error.GlobalExceptionHandler;
import com.project.investment_tracker.repository.AccountRepository;
import com.project.investment_tracker.repository.TradeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@Transactional
class TradePerformanceTests {
    @Autowired TradePerformanceService service;
    @Autowired TradePerformanceCalculator calculator;
    @Autowired AccountRepository accounts;
    @Autowired TradeRepository trades;

    private Trade trade(String symbol, TradeType type, long price, int quantity) {
        return new Trade(null, symbol, symbol, type, price, quantity,
                LocalDateTime.of(2026, 1, 1, 0, 0), "", null);
    }

    @Test
    @DisplayName("매도별 손익과 종목별 합계가 전체 손익과 일치하고 수익률은 원가로 가중 계산한다")
    void aggregatesSalesByStockWithoutAveragingRates() {
        var result = calculator.calculate(1L, List.of(
                trade("A", TradeType.BUY, 100, 10),
                trade("B", TradeType.BUY, 1000, 2),
                trade("C", TradeType.BUY, 500, 1),
                trade("A", TradeType.SELL, 120, 5),
                trade("B", TradeType.SELL, 900, 1),
                trade("A", TradeType.SELL, 110, 5)));
        assertEquals(3, result.realizedTrades().size());
        assertEquals(2, result.stocks().size());
        assertEquals(50, result.realizedProfit());
        assertEquals(new BigDecimal("2.5"), result.realizedReturnRate());
        assertEquals(150, result.stocks().get(0).realizedProfit());
        assertEquals(new BigDecimal("15.0"), result.stocks().get(0).realizedReturnRate());
        assertEquals(-100, result.stocks().get(1).realizedProfit());
        assertEquals(110, result.realizedTrades().get(0).tradePrice());
        assertEquals(50, result.realizedTrades().get(0).realizedProfit());
        assertEquals(100, result.realizedTrades().get(2).realizedProfit());
        assertEquals(result.realizedProfit(), result.stocks().stream().mapToLong(s -> s.realizedProfit()).sum());
        assertEquals(result.realizedProfit(), result.realizedTrades().stream().mapToLong(t -> t.realizedProfit()).sum());
        assertEquals(result.realizedCostBasis(), result.realizedTrades().stream().mapToLong(t -> t.realizedCostBasis()).sum());
        assertEquals(result.totalSellAmount(), result.stocks().stream().mapToLong(s -> s.totalSellAmount()).sum());
        var empty = calculator.calculate(1L, List.of());
        assertTrue(empty.stocks().isEmpty());
        assertTrue(empty.realizedTrades().isEmpty());
    }

    @Test
    @DisplayName("매도가 없으면 매수금액과 관계없이 실현손익과 수익률은 0이다")
    void noSalesHaveNoRealizedProfit() {
        var empty = calculator.calculate(1L, List.of());
        assertEquals(0, empty.realizedProfit());
        var result = calculator.calculate(1L, List.of(trade("A", TradeType.BUY, 100, 10)));
        assertEquals(1000, result.totalBuyAmount());
        assertEquals(0, result.realizedCostBasis());
        assertEquals(new BigDecimal("0.0"), result.realizedReturnRate());
    }

    @Test
    @DisplayName("종목별 이동평균 원가로 부분매도와 재매수를 계산한다")
    void handlesPartialSalesAndAdditionalBuys() {
        var result = calculator.calculate(1L, List.of(
                trade("A", TradeType.BUY, 100, 10),
                trade("B", TradeType.BUY, 1000, 2),
                trade("A", TradeType.SELL, 150, 5),
                trade("A", TradeType.BUY, 200, 5),
                trade("A", TradeType.SELL, 180, 10),
                trade("B", TradeType.SELL, 900, 1)));
        assertEquals(4000, result.totalBuyAmount());
        assertEquals(3450, result.totalSellAmount());
        assertEquals(3000, result.realizedCostBasis());
        assertEquals(450, result.realizedProfit());
        assertEquals(new BigDecimal("15.0"), result.realizedReturnRate());
    }

    @Test
    @DisplayName("전량매도 시 절삭 잔여 원가를 반영하고 재진입 시 원가를 새로 계산한다")
    void accountsForRoundingRemainderAndReentry() {
        var result = calculator.calculate(1L, List.of(
                trade("A", TradeType.BUY, 100, 1),
                trade("A", TradeType.BUY, 101, 1),
                trade("A", TradeType.SELL, 100, 1),
                trade("A", TradeType.SELL, 100, 1),
                trade("A", TradeType.BUY, 200, 1),
                trade("A", TradeType.SELL, 190, 1)));
        assertEquals(401, result.realizedCostBasis());
        assertEquals(-11, result.realizedProfit());
        assertEquals(new BigDecimal("-2.7"), result.realizedReturnRate());
    }

    @Test
    @DisplayName("int 범위를 넘는 거래금액도 long으로 계산한다")
    void largeAmounts() {
        var result = calculator.calculate(1L, List.of(
                trade("A", TradeType.BUY, 1_000_000, 3000),
                trade("A", TradeType.SELL, 1_100_000, 3000)));
        assertEquals(3_000_000_000L, result.realizedCostBasis());
        assertEquals(300_000_000L, result.realizedProfit());
    }

    @Test
    @DisplayName("API는 계좌를 분리하고 같은 시각의 거래를 ID 순서로 계산한다")
    void endpointUsesAccountAndStableChronologicalOrder() throws Exception {
        Account account = accounts.save(new Account("Performance", 0L));
        Account other = accounts.save(new Account("Other", 0L));
        LocalDateTime time = LocalDateTime.of(2026, 1, 1, 0, 0);
        trades.save(new Trade(account, "A", "A", TradeType.BUY, 100L, 10, time, "", null));
        Trade sale = trades.save(new Trade(account, "A", "A", TradeType.SELL, 120L, 5, time, "", null));
        trades.save(new Trade(other, "A", "A", TradeType.BUY, 900L, 10, time, "", null));
        var mvc = MockMvcBuilders.standaloneSetup(new TradePerformanceController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/account/{id}/performance", account.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalBuyAmount").value(1000))
                .andExpect(jsonPath("$.realizedProfit").value(100))
                .andExpect(jsonPath("$.realizedReturnRate").value(20.0));
        sale.update(TradeType.SELL, 90L, 5, time, "", null);
        assertEquals(-50, service.getPerformance(account.getId()).realizedProfit());
        trades.delete(sale);
        assertEquals(0, service.getPerformance(account.getId()).realizedProfit());
        assertEquals(0L, accounts.findById(account.getId()).orElseThrow().getCashBalance());
        mvc.perform(get("/api/account/{id}/performance", -1))
                .andExpect(status().isNotFound());
    }
}
