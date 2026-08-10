package com.project.investment_tracker.controller;

import com.project.investment_tracker.dto.PlanActionCreateRequest;
import com.project.investment_tracker.dto.PlanActionResponse;
import com.project.investment_tracker.dto.PlanActionUpdateRequest;
import com.project.investment_tracker.service.PlanActionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans/{planId}/actions")
public class PlanActionController {

    private final PlanActionService planActionService;

    public PlanActionController(PlanActionService planActionService) {
        this.planActionService = planActionService;
    }

    @PostMapping
    public PlanActionResponse createPlanAction(@PathVariable Long planId, @RequestBody PlanActionCreateRequest request) {
        return planActionService.createPlanAction(planId, request);
    }

    @GetMapping
    public List<PlanActionResponse> getPlanActions(@PathVariable Long planId) {
        return planActionService.getPlanActions(planId);
    }

    @GetMapping("/{actionId}")
    public PlanActionResponse getPlanAction(@PathVariable Long planId, @PathVariable Long actionId) {
        return planActionService.getPlanAction(planId, actionId);
    }

    @PutMapping("/{actionId}")
    public PlanActionResponse updatePlanAction(
            @PathVariable Long planId,
            @PathVariable Long actionId,
            @RequestBody PlanActionUpdateRequest request
    ) {
        return planActionService.updatePlanAction(planId, actionId, request);
    }

    @DeleteMapping("/{actionId}")
    public void deletePlanAction(@PathVariable Long planId, @PathVariable Long actionId) {
        planActionService.deletePlanAction(planId, actionId);
    }
}
