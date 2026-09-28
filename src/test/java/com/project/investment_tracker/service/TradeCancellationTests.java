package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.TradeCreateRequest;
import com.project.investment_tracker.dto.TradeUpdateRequest;
import com.project.investment_tracker.entity.Account;
import com.project.investment_tracker.entity.StockHolding;
import com.project.investment_tracker.entity.TradeType;
import com.project.investment_tracker.global.error.BadRequestException;
import com.project.investment_tracker.repository.AccountRepository;
import com.project.investment_tracker.repository.StockHoldingRepository;
import com.project.investment_tracker.repository.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:trade_cancellation",
        "kis.app-key=test", "kis.app-secret=test", "kis.base-url=http://localhost"
})
class TradeCancellationTests {
    @Autowired TradeService service;
    @Autowired AccountRepository accounts;
    @Autowired StockHoldingRepository holdings;
    @Autowired TradeRepository trades;

    private Long accountId;
    private final LocalDateTime start = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        trades.deleteAll();
        holdings.deleteAll();
        accounts.deleteAll();
        accountId = accounts.save(new Account("Test", 10000)).getId();
    }

    @Test
    @DisplayName("일부 매도 기록을 삭제하면 기존 보유 상태와 잔액이 복원된다")
    void deletingPartialSaleRestoresOriginalCost() {
        create(TradeType.BUY, 100, 10, 0);
        Long sale = create(TradeType.SELL, 200, 5, 1);
        service.deleteTrade(sale);
        assertPosition(10, 100, 1000, 9000);
    }

    @Test
    @DisplayName("전량 매도 기록을 삭제하면 기존 보유 상태와 잔액이 복원된다")
    void deletingFullSaleRestoresOriginalCost() {
        create(TradeType.BUY, 100, 10, 0);
        Long sale = create(TradeType.SELL, 200, 10, 1);
        service.deleteTrade(sale);
        assertPosition(10, 100, 1000, 9000);
    }

    @Test
    @DisplayName("추가 매수 기록을 삭제하면 이전 평균단가와 보유 상태가 복원된다")
    void deletingAdditionalBuyRestoresPreviousAverage() {
        create(TradeType.BUY, 100, 10, 0);
        Long buy = create(TradeType.BUY, 200, 5, 1);
        service.deleteTrade(buy);
        assertPosition(10, 100, 1000, 9000);
    }

    @Test
    @DisplayName("유일한 매수 기록을 삭제하면 보유 수량과 원가가 0이 되고 현금이 복원된다")
    void deletingOnlyBuyClearsPosition() {
        Long buy = create(TradeType.BUY, 100, 10, 0);
        service.deleteTrade(buy);
        assertPosition(0, 0, 0, 10000);
    }

    @Test
    @DisplayName("보유 수량을 초과하는 매도 수정은 거절되고 거래와 잔액 및 보유 상태가 유지된다")
    void invalidUpdateRollsBackCashPositionAndTrade() {
        create(TradeType.BUY, 100, 10, 0);
        Long sale = create(TradeType.SELL, 200, 5, 1);
        assertThrows(BadRequestException.class,
                () -> service.updateTrade(sale, update("TEST", TradeType.SELL, 200, 11, 1)));
        assertPosition(5, 100, 500, 10000);
        assertEquals(5, trades.findById(sale).orElseThrow().getQuantity());
    }

    @Test
    @DisplayName("같은 시각에 나중에 등록된 거래가 있으면 앞선 거래를 삭제할 수 없다")
    void laterTradeAtSameTimeBlocksCancellation() {
        Long buy = create(TradeType.BUY, 100, 10, 0);
        create(TradeType.SELL, 200, 5, 0);
        assertThrows(BadRequestException.class, () -> service.deleteTrade(buy));
        assertPosition(5, 100, 500, 10000);
    }

    @Test
    @DisplayName("매도 시각을 매수 이전으로 변경하면 거절되고 기존 잔액과 보유 상태가 유지된다")
    void movingSaleBeforeBuyIsRejected() {
        create(TradeType.BUY, 100, 10, 0);
        Long sale = create(TradeType.SELL, 200, 5, 1);
        assertThrows(BadRequestException.class,
                () -> service.updateTrade(sale, update("TEST", TradeType.SELL, 200, 5, -1)));
        assertPosition(5, 100, 500, 10000);
    }

    private Long create(TradeType type, int price, int quantity, int day) {
        return service.createTrade(new TradeCreateRequest(accountId, "Test", "TEST", type,
                price, quantity, start.plusDays(day), null, null)).id();
    }

    private TradeUpdateRequest update(String symbol, TradeType type, int price, int quantity, int day) {
        return new TradeUpdateRequest("Test", symbol, type, price, quantity,
                start.plusDays(day), null, null);
    }

    private void assertPosition(int quantity, int average, int cost, int cash) {
        StockHolding holding = holdings.findByAccountIdAndStockSymbol(accountId, "TEST").orElseThrow();
        assertEquals(quantity, holding.getQuantity());
        assertEquals(average, holding.getAveragePrice());
        assertEquals(cost, holding.getTotalInvestmentAmount());
        assertEquals(cash, accounts.findById(accountId).orElseThrow().getCashBalance());
    }
}
