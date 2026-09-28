package com.project.investment_tracker.dto;

import java.math.BigDecimal;

public record StockHoldingDashboardResponse(
        String stockName,
        String stockSymbol,
        Integer quantity,
        Long averagePrice,
        Long currentPrice,
        Long investmentAmount,
        Long evaluationAmount,
        Long profitLoss,
        BigDecimal returnRate
) {
}
