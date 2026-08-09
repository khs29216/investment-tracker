package com.project.investment_tracker.entity;

import jakarta.persistence.*;

@Entity
public class InvestmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stockName;   // 주식 이름
    private String stockSymbol; // 주식 코드

    private Integer currentPrice;   // 현재 가격
    private Integer totalBudget;    // 총 예산
    private Integer holdingQuantity;    // 가지고 있는 주식 수
    private Integer averagePrice;   // 평단가

    private String reason;  // 계획 이유

    @Enumerated(EnumType.STRING)
    private PlanStatus planStatus;
    protected InvestmentPlan() {
    }

    public InvestmentPlan(String stockName, String stockSymbol, Integer currentPrice, Integer totalBudget, Integer holdingQuantity, Integer averagePrice, String reason) {
        this.stockName = stockName;
        this.stockSymbol = stockSymbol;
        this.currentPrice = currentPrice;
        this.totalBudget = totalBudget;
        this.holdingQuantity = holdingQuantity;
        this.averagePrice = averagePrice;
        this.reason = reason;
        this.planStatus = PlanStatus.ACTIVE;
    }
    public void update(
            String stockName,
            String stockSymbol,
            Integer currentPrice,
            Integer totalBudget,
            Integer holdingQuantity,
            Integer averagePrice,
            String reason
    ) {
        this.stockName = stockName;
        this.stockSymbol = stockSymbol;
        this.currentPrice = currentPrice;
        this.totalBudget = totalBudget;
        this.holdingQuantity = holdingQuantity;
        this.averagePrice = averagePrice;
        this.reason = reason;
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

    public Integer getCurrentPrice() {
        return currentPrice;
    }

    public Integer getTotalBudget() {
        return totalBudget;
    }

    public Integer getHoldingQuantity() {
        return holdingQuantity;
    }

    public Integer getAveragePrice() {
        return averagePrice;
    }

    public String getReason() {
        return reason;
    }

    public PlanStatus getPlanStatus() {
        return planStatus;
    }
}
