package com.project.investment_tracker.entity;

import com.project.investment_tracker.global.error.BadRequestException;
import com.project.investment_tracker.global.error.ErrorMessage;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountName;

    private Long cashBalance;

    protected Account() {
    }

    public Account(String accountName, Long cashBalance) {
        this.accountName = accountName;
        this.cashBalance = cashBalance;
    }

    public void update(String accountName, Long cashBalance) {
        this.accountName = accountName;
        this.cashBalance = cashBalance;
    }

    public void decreaseCash(Long amount) {
        if (cashBalance < amount) {
            throw new BadRequestException(ErrorMessage.INSUFFICIENT_CASH_BALANCE);
        }

        this.cashBalance -= amount;
    }

    public void increaseCash(Long amount) {
        this.cashBalance += amount;
    }

    public Long getId() {
        return id;
    }

    public String getAccountName() {
        return accountName;
    }

    public Long getCashBalance() {
        return cashBalance;
    }
}
