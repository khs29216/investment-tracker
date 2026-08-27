package com.project.investment_tracker.dto;

import java.util.List;

public record AccountSummaryResponse(
        Long accountId,
        String accountName,
        Integer cashBalance,
        Integer totalInvestmentAmount,
        List<StockHoldingResponse> stockHoldings
) {
}
