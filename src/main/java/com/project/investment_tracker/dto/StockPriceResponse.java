package com.project.investment_tracker.dto;

public record StockPriceResponse(
        String stockSymbol,
        Integer currentPrice
) {
}
