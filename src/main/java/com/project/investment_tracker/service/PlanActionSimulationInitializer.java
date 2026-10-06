package com.project.investment_tracker.service;

import com.project.investment_tracker.repository.PlanActionRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class PlanActionSimulationInitializer implements ApplicationRunner {
    private final PlanActionRepository actions;
    private final PlanActionSimulationService simulations;

    public PlanActionSimulationInitializer(PlanActionRepository actions, PlanActionSimulationService simulations) {
        this.actions = actions;
        this.simulations = simulations;
    }

    @Override
    public void run(ApplicationArguments args) {
        // 기존 데이터에는 가상 체결 이력이 없으므로 미체결 상태만 보충한다.
        actions.findIdsWithoutSimulation().forEach(simulations::initializePending);
    }
}
