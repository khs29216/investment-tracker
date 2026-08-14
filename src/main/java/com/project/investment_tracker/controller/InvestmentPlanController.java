package com.project.investment_tracker.controller;

import com.project.investment_tracker.dto.InvestmentPlanCreateRequest;
import com.project.investment_tracker.dto.InvestmentPlanResponse;
import com.project.investment_tracker.dto.InvestmentPlanUpdateRequest;
import com.project.investment_tracker.service.InvestmentPlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/plans")
public class InvestmentPlanController {
    private final InvestmentPlanService investmentPlanService;

    public InvestmentPlanController(InvestmentPlanService investmentPlanService) {
        this.investmentPlanService = investmentPlanService;
    }

    @PostMapping
    public InvestmentPlanResponse createPlan(@Valid @RequestBody InvestmentPlanCreateRequest investmentPlanCreateRequest) {
        return investmentPlanService.createInvestmentPlan(investmentPlanCreateRequest);
    }

    @GetMapping
    public List<InvestmentPlanResponse> getPlans() {
        return investmentPlanService.getPlans();
    }

    @GetMapping("/{id}")
    public InvestmentPlanResponse getPlan(@PathVariable Long id) {
        return investmentPlanService.getPlan(id);
    }

    @PutMapping("/{id}")
    public InvestmentPlanResponse updatePlan(@PathVariable Long id, @Valid @RequestBody InvestmentPlanUpdateRequest investmentPlanUpdateRequest) {
        return investmentPlanService.updatePlan(id, investmentPlanUpdateRequest);
    }

    @DeleteMapping("/{id}")
    public void deletePlan(@PathVariable Long id) {
        investmentPlanService.deletePlan(id);
    }
}
