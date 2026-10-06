package com.project.investment_tracker.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import com.project.investment_tracker.global.error.BadRequestException;

@Entity
public class PlanActionSimulation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_action_id", nullable = false, unique = true)
    private PlanAction planAction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SimulationStatus status;

    private LocalDate executedDate;

    protected PlanActionSimulation() {}

    public PlanActionSimulation(PlanAction planAction) {
        this.planAction = planAction;
        this.status = SimulationStatus.PENDING;
    }

    public void execute(LocalDate date) {
        if (date == null) throw new BadRequestException("가상 체결일은 필수입니다.");
        if (status == SimulationStatus.EXECUTED) {
            if (date.equals(executedDate)) return;
            throw new BadRequestException("이미 가상 체결된 액션의 체결일은 변경할 수 없습니다.");
        }
        status = SimulationStatus.EXECUTED;
        executedDate = date;
    }

    public Long getId() { return id; }
    public PlanAction getPlanAction() { return planAction; }
    public SimulationStatus getStatus() { return status; }
    public LocalDate getExecutedDate() { return executedDate; }
}
