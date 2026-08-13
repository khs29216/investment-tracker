package com.project.investment_tracker.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stockName;
    private String stockSymbol;

    @Enumerated(EnumType.STRING)
    private TradeType tradeType;

    private Integer tradePrice;
    private Integer quantity;

    private LocalDate tradeDate;

    private String memo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_action_id")
    private PlanAction planAction;

    protected Trade() {
    }

    public Trade(
            String stockName,
            String stockSymbol,
            TradeType tradeType,
            Integer tradePrice,
            Integer quantity,
            LocalDate tradeDate,
            String memo,
            PlanAction planAction
    ) {
        this.stockName = stockName;
        this.stockSymbol = stockSymbol;
        this.tradeType = tradeType;
        this.tradePrice = tradePrice;
        this.quantity = quantity;
        this.tradeDate = tradeDate;
        this.memo = memo;
        this.planAction = planAction;
    }

    public void update(
            String stockName,
            String stockSymbol,
            TradeType tradeType,
            Integer tradePrice,
            Integer quantity,
            LocalDate tradeDate,
            String memo,
            PlanAction planAction
    ) {
        this.stockName = stockName;
        this.stockSymbol = stockSymbol;
        this.tradeType = tradeType;
        this.tradePrice = tradePrice;
        this.quantity = quantity;
        this.tradeDate = tradeDate;
        this.memo = memo;
        this.planAction = planAction;
    }

    public Long getId() {
        return id;
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

    public LocalDate getTradeDate() {
        return tradeDate;
    }

    public String getMemo() {
        return memo;
    }

    public PlanAction getPlanAction() {
        return planAction;
    }
}

