package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.TradePerformanceResponse;
import com.project.investment_tracker.dto.StockRealizedPerformanceResponse;
import com.project.investment_tracker.dto.RealizedTradeResponse;
import com.project.investment_tracker.entity.StockHolding;
import com.project.investment_tracker.entity.Trade;
import com.project.investment_tracker.entity.TradeType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;
import java.util.List;
import java.util.Map;

@Component
public class TradePerformanceCalculator {
    // 거래는 거래 시각, ID 오름차순으로 전달한다. DB의 보유 상태는 변경하지 않는다.
    public TradePerformanceResponse calculate(Long accountId, List<Trade> trades) {
        Map<String, StockHolding> positions = new HashMap<>();
        Map<String, StockRealizedPerformanceResponse> stocks = new TreeMap<>();
        List<RealizedTradeResponse> realizedTrades = new ArrayList<>();
        long totalBuyAmount = 0;
        long totalSellAmount = 0;
        long realizedCostBasis = 0;

        for (Trade trade : trades) {
            StockHolding position = positions.computeIfAbsent(trade.getStockSymbol(), symbol ->
                    new StockHolding(null, trade.getStockName(), symbol, 0, 0L));
            long amount = Math.multiplyExact(trade.getTradePrice(), trade.getQuantity().longValue());
            if (trade.getTradeType() == TradeType.BUY) {
                position.buy(trade.getTradePrice(), trade.getQuantity());
                totalBuyAmount = Math.addExact(totalBuyAmount, amount);
            } else {
                long previousCost = position.getTotalInvestmentAmount();
                position.sell(trade.getQuantity());
                // 전량 매도 시 절삭으로 남은 원가도 모두 실현 원가에 포함한다.
                long soldCost = previousCost - position.getTotalInvestmentAmount();
                realizedCostBasis = Math.addExact(realizedCostBasis, soldCost);
                totalSellAmount = Math.addExact(totalSellAmount, amount);
                long tradeProfit = Math.subtractExact(amount, soldCost);
                realizedTrades.add(new RealizedTradeResponse(trade.getId(), trade.getTradeDateTime(),
                        trade.getStockName(), trade.getStockSymbol(), trade.getQuantity(), trade.getTradePrice(),
                        amount, soldCost, tradeProfit, returnRate(tradeProfit, soldCost)));

                StockRealizedPerformanceResponse previous = stocks.get(trade.getStockSymbol());
                long stockSales = Math.addExact(previous == null ? 0 : previous.totalSellAmount(), amount);
                long stockCost = Math.addExact(previous == null ? 0 : previous.realizedCostBasis(), soldCost);
                long stockProfit = Math.subtractExact(stockSales, stockCost);
                stocks.put(trade.getStockSymbol(), new StockRealizedPerformanceResponse(
                        trade.getStockName(), trade.getStockSymbol(), stockSales, stockCost,
                        stockProfit, returnRate(stockProfit, stockCost)));
            }
        }

        long profit = Math.subtractExact(totalSellAmount, realizedCostBasis);
        Collections.reverse(realizedTrades);
        return new TradePerformanceResponse(accountId, totalBuyAmount, totalSellAmount,
                realizedCostBasis, profit, returnRate(profit, realizedCostBasis),
                List.copyOf(stocks.values()), List.copyOf(realizedTrades));
    }

    private BigDecimal returnRate(long profit, long realizedCostBasis) {
        return realizedCostBasis == 0 ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(profit).multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(realizedCostBasis), 1, RoundingMode.HALF_UP);
    }
}
