package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.ActionStatus;
import com.project.investment_tracker.entity.ActionType;
import com.project.investment_tracker.entity.PlanAction;

public record PlanActionResponse(
        Long id,
        Long investmentPlanId,
        ActionType actionType,
        Long triggerPrice,
        Integer quantity,
        String memo,
        ActionStatus actionStatus,
        com.project.investment_tracker.entity.SimulationStatus simulationStatus,
        java.time.LocalDate executedDate
) {
    public static PlanActionResponse from(PlanAction planAction) {
        return new PlanActionResponse(
                planAction.getId(),
                planAction.getInvestmentPlan().getId(),
                planAction.getActionType(),
                planAction.getTriggerPrice(),
                planAction.getQuantity(),
                planAction.getMemo(),
                planAction.getActionStatus(),
                planAction.getSimulation() == null ? com.project.investment_tracker.entity.SimulationStatus.PENDING
                        : planAction.getSimulation().getStatus(),
                planAction.getSimulation() == null ? null : planAction.getSimulation().getExecutedDate()
        );
    }
}
