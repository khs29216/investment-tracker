package com.project.investment_tracker.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class CashTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    private CashTransactionType type;

    private Integer amount;

    private LocalDate transactionDate;

    private String memo;

    protected CashTransaction() {
    }

    public CashTransaction(
            Account account,
            CashTransactionType type,
            Integer amount,
            LocalDate transactionDate,
            String memo
    ) {
        this.account = account;
        this.type = type;
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.memo = memo;
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public CashTransactionType getType() {
        return type;
    }

    public Integer getAmount() {
        return amount;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public String getMemo() {
        return memo;
    }
}
