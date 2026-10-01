package com.project.investment_tracker.dto;

import java.math.BigDecimal;

public record StockRealizedPerformanceResponse(
        String stockName, String stockSymbol, long totalSellAmount,
        long realizedCostBasis, long realizedProfit, BigDecimal realizedReturnRate
) {
}
