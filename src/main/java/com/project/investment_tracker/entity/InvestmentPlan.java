package com.project.investment_tracker.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;
import com.project.investment_tracker.global.error.BadRequestException;

@Entity
public class InvestmentPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String stockName;   // 주식 이름
    private String stockSymbol; // 주식 코드

    private Long totalBudget;    // 총 예산

    private String reason;  // 계획 이유

    @Enumerated(EnumType.STRING)
    private PlanStatus planStatus;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;
    private Integer initialQuantity;
    private Long initialCostBasis;
    private Long initialPrice;
    private Long initialCash;
    private LocalDateTime startedAt;
    private LocalDate plannedEndDate;
    private LocalDateTime endedAt;
    protected InvestmentPlan() {
    }

    public InvestmentPlan(String stockName, String stockSymbol, Long totalBudget, String reason) {
        this.stockName = stockName;
        this.stockSymbol = stockSymbol;
        this.totalBudget = totalBudget;
        this.reason = reason;
        this.planStatus = PlanStatus.ACTIVE;
    }

    public InvestmentPlan(Account account, String stockName, String stockSymbol, Long budget, String reason,
                          int quantity, long costBasis, long price, LocalDateTime startedAt, LocalDate endDate) {
        this(stockName, stockSymbol, budget, reason);
        this.account = account;
        this.initialQuantity = quantity;
        this.initialCostBasis = costBasis;
        this.initialPrice = price;
        this.initialCash = budget;
        this.startedAt = startedAt;
        this.plannedEndDate = endDate;
    }

    public void close(LocalDateTime now) {
        if (startedAt == null) throw new BadRequestException("초기 상태가 없는 기존 계획은 종료 분석 대상이 아닙니다.");
        if (getPlanStatus() != PlanStatus.ACTIVE) throw new BadRequestException("이미 종료된 계획입니다.");
        this.endedAt = now;
        this.planStatus = PlanStatus.CANCELLED;
    }

    public Account getAccount() { return account; }
    public Integer getInitialQuantity() { return initialQuantity; }
    public Long getInitialCostBasis() { return initialCostBasis; }
    public Long getInitialPrice() { return initialPrice; }
    public Long getInitialCash() { return initialCash; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDate getPlannedEndDate() { return plannedEndDate; }
    public LocalDateTime getEndedAt() {
        if (endedAt != null) return endedAt;
        return getPlanStatus() == PlanStatus.COMPLETED && plannedEndDate != null
                ? plannedEndDate.atTime(java.time.LocalTime.MAX) : null;
    }
    public void update(
            String stockName,
            String stockSymbol,
            Long totalBudget,
            String reason
    ) {
        if (startedAt != null && (getPlanStatus() != PlanStatus.ACTIVE
                || !Objects.equals(this.stockName, stockName) || !Objects.equals(this.stockSymbol, stockSymbol)
                || !Objects.equals(this.totalBudget, totalBudget))) {
            throw new BadRequestException("시작한 계획의 종목과 예산은 변경할 수 없으며 종료된 계획은 수정할 수 없습니다.");
        }
        this.stockName = stockName;
        this.stockSymbol = stockSymbol;
        this.totalBudget = totalBudget;
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

    public Long getTotalBudget() {
        return totalBudget;
    }

    public String getReason() {
        return reason;
    }

    public PlanStatus getPlanStatus() {
        if (planStatus == PlanStatus.ACTIVE && plannedEndDate != null
                && LocalDate.now(ZoneId.of("Asia/Seoul")).isAfter(plannedEndDate)) return PlanStatus.COMPLETED;
        return planStatus;
    }
}
