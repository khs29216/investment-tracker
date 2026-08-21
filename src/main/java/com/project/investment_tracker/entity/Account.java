package com.project.investment_tracker.entity;

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
