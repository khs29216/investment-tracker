package com.project.investment_tracker.dto;

import java.util.List;

public record AccountSummaryResponse(
        Long accountId,
        String accountName,
        Long cashBalance,
        Long totalInvestmentAmount,
        List<StockHoldingResponse> stockHoldings
) {
}
