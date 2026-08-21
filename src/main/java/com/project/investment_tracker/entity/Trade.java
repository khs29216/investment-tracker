package com.project.investment_tracker.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    private String stockName;
    private String stockSymbol;

    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    private Integer tradePrice;
    private Integer quantity;

    private LocalDateTime tradeDateTime;

    private String memo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_action_id")
    private PlanAction planAction;

    protected Trade() {
    }

    public Trade(
            Account account,
            String stockName,
            String stockSymbol,
            TradeType tradeType,
            Integer tradePrice,
            Integer quantity,
            LocalDateTime tradeDateTime,
            String memo,
            PlanAction planAction
    ) {
        this.account = account;
        this.stockName = stockName;
        this.stockSymbol = stockSymbol;
        this.tradeType = tradeType;
        this.tradePrice = tradePrice;
        this.quantity = quantity;
        this.tradeDateTime = tradeDateTime;
        this.memo = memo;
        this.planAction = planAction;
    }

    public void update(
            String stockName,
            String stockSymbol,
            TradeType tradeType,
            Integer tradePrice,
            Integer quantity,
            LocalDateTime tradeDateTime,
            String memo,
            PlanAction planAction
    ) {
        this.stockName = stockName;
        this.stockSymbol = stockSymbol;
        this.tradeType = tradeType;
        this.tradePrice = tradePrice;
        this.quantity = quantity;
        this.tradeDateTime = tradeDateTime;
        this.memo = memo;
        this.planAction = planAction;
    }

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public String getStockName() {
        return stockName;
    }

    public String getStockSymbol() {
        return stockSymbol;
    }

    public TradeType getTradeType() {
        return tradeType;
    }

    public Integer getTradePrice() {
        return tradePrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LocalDateTime getTradeDateTime() {
        return tradeDateTime;
    }

    public String getMemo() {
        return memo;
    }

    public PlanAction getPlanAction() {
        return planAction;
    }
}

