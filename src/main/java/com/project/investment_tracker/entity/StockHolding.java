package com.project.investment_tracker.entity;

import jakarta.persistence.*;

@Entity
public class StockHolding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    private String stockName;

    private String stockSymbol;

    private Integer quantity;

    private Integer averagePrice;

    private Integer totalInvestmentAmount;

    protected StockHolding() {
    }

    public StockHolding(
            Account account,
            String stockName,
            String stockSymbol,
            Integer quantity,
            Integer averagePrice
    ) {
        this.account = account;
        this.stockName = stockName;
        this.stockSymbol = stockSymbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.totalInvestmentAmount = averagePrice * quantity;
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

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getAveragePrice() {
        return averagePrice;
    }

    public Integer getTotalInvestmentAmount() {
        return totalInvestmentAmount;
    }

}
