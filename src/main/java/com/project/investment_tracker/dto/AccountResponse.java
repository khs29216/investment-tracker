package com.project.investment_tracker.dto;

import com.project.investment_tracker.entity.Account;

public record AccountResponse(
        Long id,
        String accountName,
        Integer cashBalance
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountName(),
                account.getCashBalance()
        );
    }
}
