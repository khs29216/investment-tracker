package com.project.investment_tracker.dto;

import java.math.BigDecimal;
import java.util.List;

public record AccountDashboardResponse(
        Long accountId,
        String accountName,
        Integer cashBalance,
        Integer totalInvestmentAmount,
        Integer totalEvaluationAmount,
        Integer totalProfitLoss,
        BigDecimal totalReturnRate,
        List<StockHoldingDashboardResponse> stockHoldings
) {
}
