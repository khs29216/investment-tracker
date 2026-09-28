package com.project.investment_tracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record AccountDashboardResponse(
        Long accountId,
        String accountName,
        Long cashBalance,
        Long totalInvestmentAmount,
        Long totalEvaluationAmount,
        Long totalProfitLoss,
        BigDecimal totalReturnRate,
        List<StockHoldingDashboardResponse> stockHoldings
) {
}
