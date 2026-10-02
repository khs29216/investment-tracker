package com.project.investment_tracker.dto;

import java.time.LocalDate;

public record StockDailyPriceResponse(
        String stockSymbol, LocalDate date, long open, long high, long low, long close
) {
}
