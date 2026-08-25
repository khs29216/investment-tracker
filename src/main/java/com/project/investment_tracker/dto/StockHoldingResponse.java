package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.StockHolding;

public record StockHoldingResponse(
        Long accountId,
        String stockName,
        String stockSymbol,
        Integer quantity,
        Integer averagePrice,
        Integer totalInvestmentAmount
) {
    public static StockHoldingResponse from(StockHolding stockHolding) {
        return new StockHoldingResponse(
                stockHolding.getAccount().getId(),
                stockHolding.getStockName(),
                stockHolding.getStockSymbol(),
                stockHolding.getQuantity(),
                stockHolding.getAveragePrice(),
                stockHolding.getTotalInvestmentAmount()
        );
    }
}
