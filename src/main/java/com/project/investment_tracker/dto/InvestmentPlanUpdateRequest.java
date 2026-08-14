package com.project.investment_tracker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record InvestmentPlanUpdateRequest(
        @NotBlank(message = "종목명은 필수입니다.")
        String stockName,
        @NotBlank(message = "종목 코드는 필수입니다.")
        String stockSymbol,
        @NotNull(message = "현재가는 필수입니다.")
        @Positive(message = "현재가는 양수여야 합니다.")
        Integer currentPrice,
        @NotNull(message = "총 예산은 필수입니다.")
        @Positive(message = "총 예산은 양수여야 합니다.")
        Integer totalBudget,
        @NotNull(message = "보유 수량은 필수입니다.")
        @PositiveOrZero(message = "보유 수량은 0 이상이어야 합니다.")
        Integer holdingQuantity,
        @NotNull(message = "평균 단가는 필수입니다.")
        @PositiveOrZero(message = "평균 단가는 0 이상이어야 합니다.")
        Integer averagePrice,
        @NotBlank(message = "계획 이유는 필수입니다.")
        String reason
) {
}
