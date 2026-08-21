package com.project.investment_tracker.entity;

import com.project.investment_tracker.global.error.BadRequestException;
import com.project.investment_tracker.global.error.ErrorMessage;
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

    public void buy(Integer price, Integer quantity) {
        int additionalAmount = price * quantity;
        int updatedTotalInvestmentAmount = this.totalInvestmentAmount + additionalAmount;
        int updatedQuantity = this.quantity + quantity;

        this.quantity = updatedQuantity;
        this.totalInvestmentAmount = updatedTotalInvestmentAmount;
        this.averagePrice = updatedTotalInvestmentAmount / updatedQuantity;
    }

    public void sell(Integer quantity) {
        if (this.quantity < quantity) {
            throw new BadRequestException(ErrorMessage.INSUFFICIENT_STOCK_QUANTITY);
        }

        int soldInvestmentAmount = this.averagePrice * quantity;

        this.quantity -= quantity;
        this.totalInvestmentAmount -= soldInvestmentAmount;

        if (this.quantity == 0) {
            this.averagePrice = 0;
            this.totalInvestmentAmount = 0;
        }
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
