package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.Trade;
import com.project.investment_tracker.entity.TradeType;

import java.time.LocalDate;

public record TradeResponse(
        Long id,
        String stockName,
        String stockSymbol,
        TradeType tradeType,
        Integer tradePrice,
        Integer quantity,
        LocalDate tradeDate,
        String memo,
        Long planActionId
) {
    public static TradeResponse from(Trade trade) {
        Long planActionId = null;

        if (trade.getPlanAction() != null) {
            planActionId = trade.getPlanAction().getId();
        }

        return new TradeResponse(
                trade.getId(),
                trade.getStockName(),
                trade.getStockSymbol(),
                trade.getTradeType(),
                trade.getTradePrice(),
                trade.getQuantity(),
                trade.getTradeDate(),
                trade.getMemo(),
                planActionId
        );
    }
}
