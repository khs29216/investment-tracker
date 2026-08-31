package com.project.investment_tracker.dto;

import java.math.BigDecimal;

public record StockHoldingDashboardResponse(
        String stockName,
        String stockSymbol,
        Integer quantity,
        Integer averagePrice,
        Integer currentPrice,
        Integer investmentAmount,
        Integer evaluationAmount,
        Integer profitLoss,
        BigDecimal returnRate
) {
}
