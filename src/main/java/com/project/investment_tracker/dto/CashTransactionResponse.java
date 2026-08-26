package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.CashTransaction;
import com.project.investment_tracker.entity.CashTransactionType;

import java.time.LocalDateTime;

public record CashTransactionResponse(
        Long id,
        Long accountId,
        CashTransactionType type,
        Integer amount,
        LocalDateTime transactionDateTime,
        String memo
) {
    public static CashTransactionResponse from(CashTransaction cashTransaction) {
        return new CashTransactionResponse(
                cashTransaction.getId(),
                cashTransaction.getAccount().getId(),
                cashTransaction.getType(),
                cashTransaction.getAmount(),
                cashTransaction.getTransactionDateTime(),
                cashTransaction.getMemo()
        );
    }
}
