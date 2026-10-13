package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.ActionType;
import com.project.investment_tracker.service.VirtualExecutionCalculator;
import java.time.LocalDate;
import java.util.List;

public record PlanSimulationResponse(Long planId, long cash, int quantity, long costBasis,
                                     long averagePrice, long realizedProfit, LocalDate processedThrough,
                                     List<Execution> executions, List<SkippedAction> skippedActions) {
    public static PlanSimulationResponse from(Long planId, VirtualExecutionCalculator.Result result) {
        var state = result.state();
        return new PlanSimulationResponse(planId, state.cash(), state.quantity(), state.costBasis(),
                state.averagePrice(), state.realizedProfit(), state.processedThrough(),
                result.executions().stream().map(item -> new Execution(item.actionId(), item.date(), item.type(),
                        item.price(), item.quantity(), item.amount(), item.costBasis(), item.realizedProfit())).toList(),
                result.skippedActions().stream().map(item -> new SkippedAction(item.actionId(), item.date(),
                        item.reason().name())).toList());
    }

    public record Execution(long actionId, LocalDate date, ActionType type, long price, int quantity,
                            long amount, long costBasis, long realizedProfit) {}
    public record SkippedAction(long actionId, LocalDate date, String reason) {}
}
