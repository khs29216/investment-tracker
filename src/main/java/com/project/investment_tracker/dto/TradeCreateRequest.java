package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.TradeType;

import java.time.LocalDate;

public record TradeCreateRequest(
        String stockName,
        String stockSymbol,
        TradeType tradeType,
        Integer tradePrice,
        Integer quantity,
        LocalDate tradeDate,
        String memo,
        Long planActionId
) {
}
