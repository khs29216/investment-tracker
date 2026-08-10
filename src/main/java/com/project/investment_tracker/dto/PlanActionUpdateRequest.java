package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.ActionType;

public record PlanActionUpdateRequest(
        ActionType actionType,
        Integer triggerPrice,
        Integer quantity,
        String memo
) {
}
