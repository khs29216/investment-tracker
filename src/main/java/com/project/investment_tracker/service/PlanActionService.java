package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.PlanActionCreateRequest;
import com.project.investment_tracker.dto.PlanActionResponse;
import com.project.investment_tracker.dto.PlanActionUpdateRequest;
import com.project.investment_tracker.entity.InvestmentPlan;
import com.project.investment_tracker.entity.PlanAction;
import com.project.investment_tracker.global.error.ErrorMessage;
import com.project.investment_tracker.global.error.InvalidRelationException;
import com.project.investment_tracker.global.error.ResourceNotFoundException;
import com.project.investment_tracker.repository.InvestmentPlanRepository;
import com.project.investment_tracker.repository.PlanActionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PlanActionService {

    private final PlanActionRepository planActionRepository;
    private final InvestmentPlanRepository investmentPlanRepository;

    public PlanActionService(PlanActionRepository planActionRepository, InvestmentPlanRepository investmentPlanRepository) {
        this.planActionRepository = planActionRepository;
        this.investmentPlanRepository = investmentPlanRepository;
    }

    private PlanAction findActionInPlan(Long planId, Long actionId) {
        PlanAction planAction = planActionRepository.findById(actionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PLAN_ACTION_NOT_FOUND));

        Long actualPlanId = planAction.getInvestmentPlan().getId();

        if (!actualPlanId.equals(planId)) {
            throw new InvalidRelationException(ErrorMessage.PLAN_ACTION_NOT_BELONG_TO_PLAN);
        }

        return planAction;
    }

    public PlanActionResponse createPlanAction(Long investmentPlanId, PlanActionCreateRequest request) {
        InvestmentPlan investmentPlan = investmentPlanRepository.findById(investmentPlanId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVESTMENT_PLAN_NOT_FOUND));

        PlanAction planAction = new PlanAction(
                investmentPlan,
                request.actionType(),
                request.triggerPrice(),
                request.quantity(),
                request.memo()
        );

        PlanAction savedPlanAction = planActionRepository.save(planAction);

        return PlanActionResponse.from(savedPlanAction);
    }

    public List<PlanActionResponse> getPlanActions(Long investmentPlanId) {
        return planActionRepository.findByInvestmentPlanId(investmentPlanId)
                .stream()
                .map(PlanActionResponse::from)
                .toList();
    }

    public PlanActionResponse getPlanAction(Long planId, Long actionId) {
        PlanAction planAction = findActionInPlan(planId, actionId);

        return PlanActionResponse.from(planAction);
    }

    @Transactional
    public PlanActionResponse updatePlanAction(Long planId, Long actionId, PlanActionUpdateRequest request) {
        PlanAction planAction = findActionInPlan(planId, actionId);

        planAction.update(
                request.actionType(),
                request.triggerPrice(),
                request.quantity(),
                request.memo()
        );

        return PlanActionResponse.from(planAction);
    }

    public void deletePlanAction(Long planId, Long actionId) {
        PlanAction actionInPlan = findActionInPlan(planId, actionId);
        planActionRepository.delete(actionInPlan);
    }

}
