package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.InvestmentPlan;
import com.project.investment_tracker.entity.PlanStatus;

public record InvestmentPlanResponse (
        Long id,
        String stockName,
        String stockSymbol,
        Long totalBudget,
        String reason,
        PlanStatus planStatus,
        Long accountId, Integer initialQuantity, Long initialCostBasis, Long initialPrice, Long initialCash,
        java.time.LocalDateTime startedAt, java.time.LocalDate plannedEndDate, java.time.LocalDateTime endedAt
) {
    public static InvestmentPlanResponse from(InvestmentPlan investmentPlan) {
        return new InvestmentPlanResponse(
                investmentPlan.getId(),
                investmentPlan.getStockName(),
                investmentPlan.getStockSymbol(),
                investmentPlan.getTotalBudget(),
                investmentPlan.getReason(),
                investmentPlan.getPlanStatus(),
                investmentPlan.getAccount() == null ? null : investmentPlan.getAccount().getId(),
                investmentPlan.getInitialQuantity(), investmentPlan.getInitialCostBasis(),
                investmentPlan.getInitialPrice(), investmentPlan.getInitialCash(),
                investmentPlan.getStartedAt(), investmentPlan.getPlannedEndDate(), investmentPlan.getEndedAt()
        );
    }
}
