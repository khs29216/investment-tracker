package com.project.investment_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record AccountCreateRequest(
        @NotBlank(message = "계좌명은 필수입니다.")
        String accountName,

        @NotNull(message = "현금 잔고는 필수입니다.")
        @PositiveOrZero(message = "현금 잔고는 0 이상이어야 합니다.")
        Integer cashBalance
) {
}
