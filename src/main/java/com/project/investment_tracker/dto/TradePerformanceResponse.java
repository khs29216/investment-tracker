package com.project.investment_tracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record TradePerformanceResponse(
        Long accountId,
        long totalBuyAmount,
        long totalSellAmount,
        long realizedCostBasis,
        long realizedProfit,
        BigDecimal realizedReturnRate,
        List<StockRealizedPerformanceResponse> stocks,
        List<RealizedTradeResponse> realizedTrades
) {
}
