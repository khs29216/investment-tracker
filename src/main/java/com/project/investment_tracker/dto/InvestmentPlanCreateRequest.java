package com.project.investment_tracker.dto;

public record InvestmentPlanCreateRequest(
        String stockName,
        String stockSymbol,
        Integer currentPrice,
        Integer totalBudget,
        Integer holdingQuantity,
        Integer averagePrice,
        String reason
) {
}
