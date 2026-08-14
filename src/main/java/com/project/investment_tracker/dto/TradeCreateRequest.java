package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.TradeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record TradeCreateRequest(
        @NotBlank(message = "종목명은 필수입니다.")
        String stockName,
        @NotBlank(message = "종목 코드는 필수입니다.")
        String stockSymbol,
        @NotNull(message = "거래 타입은 필수입니다.")
        TradeType tradeType,
        @NotNull(message = "거래 가격은 필수입니다.")
        @Positive(message = "거래 가격은 양수여야 합니다.")
        Integer tradePrice,
        @NotNull(message = "거래 수량은 필수입니다.")
        @Positive(message = "거래 수량은 양수여야 합니다.")
        Integer quantity,
        @NotNull(message = "거래일은 필수입니다.")
        LocalDate tradeDate,
        String memo,
        Long planActionId
) {
}
