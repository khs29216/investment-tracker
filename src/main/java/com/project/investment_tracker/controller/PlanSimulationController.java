package com.project.investment_tracker.controller;

import com.project.investment_tracker.dto.PlanSimulationResponse;
import com.project.investment_tracker.service.PlanSimulationService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans/{planId}/simulation")
public class PlanSimulationController {
    private final PlanSimulationService simulations;

    public PlanSimulationController(PlanSimulationService simulations) {
        this.simulations = simulations;
    }

    @PostMapping
    public PlanSimulationResponse simulate(@PathVariable Long planId) {
        return PlanSimulationResponse.from(planId, simulations.simulate(planId));
    }
}
