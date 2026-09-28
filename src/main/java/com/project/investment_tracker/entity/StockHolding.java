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

    private Long averagePrice;

    private Long totalInvestmentAmount;

    protected StockHolding() {
    }

    public StockHolding(
            Account account,
            String stockName,
            String stockSymbol,
            Integer quantity,
            Long averagePrice
    ) {
        this.account = account;
        this.stockName = stockName;
        this.stockSymbol = stockSymbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.totalInvestmentAmount = averagePrice * quantity;
    }

    public void buy(Long price, Integer quantity) {
        long additionalAmount = price * quantity;
        long updatedTotalInvestmentAmount = this.totalInvestmentAmount + additionalAmount;
        int updatedQuantity = this.quantity + quantity;

        this.quantity = updatedQuantity;
        this.totalInvestmentAmount = updatedTotalInvestmentAmount;
        // 평균단가는 원 단위로 절삭하고, 투자원금은 실제 매수 금액을 유지한다.
        this.averagePrice = updatedTotalInvestmentAmount / updatedQuantity;
    }

    public void sell(Integer quantity) {
        if (this.quantity < quantity) {
            throw new BadRequestException(ErrorMessage.INSUFFICIENT_STOCK_QUANTITY);
        }

        long soldInvestmentAmount = this.averagePrice * quantity;

        this.quantity -= quantity;
        this.totalInvestmentAmount -= soldInvestmentAmount;

        if (this.quantity == 0) {
            this.averagePrice = 0L;
            this.totalInvestmentAmount = 0L;
        }
    }

    public Long getId() {
        return id;
    }

    public void resetPosition() {
        this.quantity = 0;
        this.averagePrice = 0L;
        this.totalInvestmentAmount = 0L;
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

    public Long getAveragePrice() {
        return averagePrice;
    }

    public Long getTotalInvestmentAmount() {
        return totalInvestmentAmount;
    }

}
