package com.project.investment_tracker.service;


import com.project.investment_tracker.dto.InvestmentPlanCreateRequest;
import com.project.investment_tracker.dto.InvestmentPlanResponse;
import com.project.investment_tracker.dto.InvestmentPlanUpdateRequest;
import com.project.investment_tracker.entity.InvestmentPlan;
import com.project.investment_tracker.repository.InvestmentPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InvestmentPlanService {
    private final InvestmentPlanRepository investmentPlanRepository;

    public InvestmentPlanService(InvestmentPlanRepository investmentPlanRepository) {
        this.investmentPlanRepository = investmentPlanRepository;
    }

    public InvestmentPlanResponse createInvestmentPlan(InvestmentPlanCreateRequest request) {
        InvestmentPlan investmentPlan = new InvestmentPlan(
                request.stockName(),
                request.stockSymbol(),
                request.currentPrice(),
                request.totalBudget(),
                request.holdingQuantity(),
                request.averagePrice(),
                request.reason()
        );
        InvestmentPlan savedInvestmentPlan = investmentPlanRepository.save(investmentPlan);

        return InvestmentPlanResponse.from(savedInvestmentPlan);
    }

    public List<InvestmentPlanResponse> getPlans() {
        return investmentPlanRepository.findAll()
                .stream()
                .map(InvestmentPlanResponse::from)
                .toList();
    }

    public InvestmentPlanResponse getPlan(Long id) {
        InvestmentPlan investmentPlan = investmentPlanRepository.findById(id)
                .orElseThrow();

        return InvestmentPlanResponse.from(investmentPlan);
    }

    @Transactional
    public InvestmentPlanResponse updatePlan(Long id, InvestmentPlanUpdateRequest request) {
        InvestmentPlan investmentPlan = investmentPlanRepository.findById(id)
                .orElseThrow();

        investmentPlan.update(
                request.stockName(),
                request.stockSymbol(),
                request.currentPrice(),
                request.totalBudget(),
                request.holdingQuantity(),
                request.averagePrice(),
                request.reason()

        );

        return InvestmentPlanResponse.from(investmentPlan);
    }

    public void deletePlan(Long id) {
        investmentPlanRepository.deleteById(id);
    }
}
