package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.CashTransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CashTransactionCreateRequest(
        @NotNull(message = "계좌 ID는 필수입니다.")
        Long accountId,

        @NotNull(message = "입출금 타입은 필수입니다.")
        CashTransactionType type,

        @NotNull(message = "금액은 필수입니다.")
        @Positive(message = "금액은 양수여야 합니다.")
        Integer amount,

        String memo
) {
}
