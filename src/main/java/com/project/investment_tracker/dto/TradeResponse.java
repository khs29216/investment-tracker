package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.Trade;
import com.project.investment_tracker.entity.TradeType;
import java.time.LocalDateTime;

public record TradeResponse(
        Long id,
        Long accountId,
        String stockName,
        String stockSymbol,
        TradeType tradeType,
        Integer tradePrice,
        Integer quantity,
        LocalDateTime tradeDateTime,
        String memo,
        Long planActionId
) {
    public static TradeResponse from(Trade trade) {
        Long accountId = null;
        Long planActionId = null;

        if (trade.getAccount() != null) {
            accountId = trade.getAccount().getId();
        }

        if (trade.getPlanAction() != null) {
            planActionId = trade.getPlanAction().getId();
        }

        return new TradeResponse(
                trade.getId(),
                accountId,
                trade.getStockName(),
                trade.getStockSymbol(),
                trade.getTradeType(),
                trade.getTradePrice(),
                trade.getQuantity(),
                trade.getTradeDateTime(),
                trade.getMemo(),
                planActionId
        );
    }
}
