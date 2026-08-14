package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.ActionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlanActionUpdateRequest(
        @NotNull(message = "액션 타입은 필수입니다.")
        ActionType actionType,
        @NotNull(message = "기준 가격은 필수입니다.")
        @Positive(message = "기준 가격은 양수여야 합니다.")
        Integer triggerPrice,
        @NotNull(message = "수량은 필수입니다.")
        @Positive(message = "수량은 양수여야 합니다.")
        Integer quantity,
        String memo
) {
}
