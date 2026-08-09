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

    private Integer triggerPrice;
    private Integer quantity;

    private String memo;

    @Enumerated(EnumType.STRING)
    private ActionStatus actionStatus;

    protected PlanAction() {
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

    public Integer getTriggerPrice() {
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
