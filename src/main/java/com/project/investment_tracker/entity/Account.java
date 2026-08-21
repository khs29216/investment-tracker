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

    private Integer cashBalance;

    protected Account() {
    }

    public Account(String accountName, Integer cashBalance) {
        this.accountName = accountName;
        this.cashBalance = cashBalance;
    }

    public void update(String accountName, Integer cashBalance) {
        this.accountName = accountName;
        this.cashBalance = cashBalance;
    }

    public void decreaseCash(Integer amount) {
        if (cashBalance < amount) {
            throw new BadRequestException(ErrorMessage.INSUFFICIENT_CASH_BALANCE);
        }

        this.cashBalance -= amount;
    }

    public void increaseCash(Integer amount) {
        this.cashBalance += amount;
    }

    public Long getId() {
        return id;
    }

    public String getAccountName() {
        return accountName;
    }

    public Integer getCashBalance() {
        return cashBalance;
    }
}
