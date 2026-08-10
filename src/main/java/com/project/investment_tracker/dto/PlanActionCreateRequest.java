package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.ActionType;

public record PlanActionCreateRequest(
        ActionType actionType,
        Integer triggerPrice,
        Integer quantity,
        String memo
) {
}
