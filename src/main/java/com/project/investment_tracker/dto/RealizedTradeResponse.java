package com.project.investment_tracker.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RealizedTradeResponse(
        Long tradeId, LocalDateTime tradeDateTime, String stockName, String stockSymbol,
        int quantity, long tradePrice, long sellAmount, long realizedCostBasis,
        long realizedProfit, BigDecimal realizedReturnRate
) {
}
