package com.project.investment_tracker.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    private LocalDateTime createdAt;
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
        this.createdAt = startedAt;
        this.plannedEndDate = endDate;
    }

    public static InvestmentPlan draft(Account account, String name, String symbol, Long budget,
                                       String reason, LocalDate endDate, LocalDateTime now) {
        var plan = new InvestmentPlan(name, symbol, budget, reason);
        plan.account = account;
        plan.planStatus = PlanStatus.DRAFT;
        plan.plannedEndDate = endDate;
        plan.createdAt = now;
        return plan;
    }

    public void requireDraft() {
        if (planStatus != PlanStatus.DRAFT) throw new BadRequestException("작성 중인 계획만 변경할 수 있습니다.");
    }

    public void start(int quantity, long costBasis, long price, LocalDateTime now) {
        requireDraft();
        if (plannedEndDate == null || !plannedEndDate.isAfter(now.toLocalDate()))
            throw new BadRequestException("예정 종료일은 시작일 이후여야 합니다.");
        initialQuantity = quantity;
        initialCostBasis = costBasis;
        initialPrice = price;
        initialCash = totalBudget;
        startedAt = now;
        planStatus = PlanStatus.ACTIVE;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void changeEndDate(LocalDate date) { requireDraft(); plannedEndDate = date; }

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
        requireDraft();
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
