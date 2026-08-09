package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.InvestmentPlan;
import com.project.investment_tracker.entity.PlanStatus;

public record InvestmentPlanResponse (
        Long id,
        String stockName,
        String stockSymbol,
        Integer currentPrice,
        Integer totalBudget,
        Integer holdingQuantity,
        Integer averagePrice,
        String reason,
        PlanStatus planStatus
) {
    public static InvestmentPlanResponse from(InvestmentPlan investmentPlan) {
        return new InvestmentPlanResponse(
                investmentPlan.getId(),
                investmentPlan.getStockName(),
                investmentPlan.getStockSymbol(),
                investmentPlan.getCurrentPrice(),
                investmentPlan.getTotalBudget(),
                investmentPlan.getHoldingQuantity(),
                investmentPlan.getAveragePrice(),
                investmentPlan.getReason(),
                investmentPlan.getPlanStatus()
        );
    }
}
