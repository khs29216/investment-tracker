package com.project.investment_tracker.entity;

import jakarta.persistence.*;

@Entity
public class PlanAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "investment_plan_id", nullable = false)
    private InvestmentPlan investmentPlan;

    @Enumerated(EnumType.STRING)
    private ActionType actionType;

    private Long triggerPrice;
    private Integer quantity;

    private String memo;

    @Enumerated(EnumType.STRING)
    private ActionStatus actionStatus;

    @OneToOne(mappedBy = "planAction", cascade = CascadeType.ALL, orphanRemoval = true)
    private PlanActionSimulation simulation;

    protected PlanAction() {
    }

    public PlanAction(
            InvestmentPlan investmentPlan,
            ActionType actionType,
            Long triggerPrice,
            Integer quantity,
            String memo
    ) {
        this.investmentPlan = investmentPlan;
        this.actionType = actionType;
        this.triggerPrice = triggerPrice;
        this.quantity = quantity;
        this.memo = memo;
        this.actionStatus = ActionStatus.PENDING;
        this.simulation = new PlanActionSimulation(this);
    }

    public void update(
            ActionType actionType,
            Long triggerPrice,
            Integer quantity,
            String memo
    ) {
        this.actionType = actionType;
        this.triggerPrice = triggerPrice;
        this.quantity = quantity;
        this.memo = memo;
    }

    public void updateExecutionStatus(long executedQuantity) {
        if (executedQuantity == 0) {
            this.actionStatus = ActionStatus.PENDING;
        } else if (executedQuantity < this.quantity) {
            this.actionStatus = ActionStatus.IN_PROGRESS;
        } else {
            this.actionStatus = ActionStatus.EXECUTED;
        }
    }

    public PlanActionSimulation getSimulation() { return simulation; }

    public PlanActionSimulation initializeSimulation() {
        if (simulation == null) simulation = new PlanActionSimulation(this);
        return simulation;
    }

    public Long getId() {
        return id;
    }

    public InvestmentPlan getInvestmentPlan() {
        return investmentPlan;
    }

    public ActionType getActionType() {
        return actionType;
    }

    public Long getTriggerPrice() {
        return triggerPrice;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getMemo() {
        return memo;
    }

    public ActionStatus getActionStatus() {
        return actionStatus;
    }

}
