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
import com.project.investment_tracker.repository.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import com.project.investment_tracker.entity.SimulationStatus;
import com.project.investment_tracker.global.error.BadRequestException;

@Service
public class PlanActionService {

    private final PlanActionRepository planActionRepository;
    private final InvestmentPlanRepository investmentPlanRepository;
    private final TradeRepository tradeRepository;

    public PlanActionService(PlanActionRepository planActionRepository, InvestmentPlanRepository investmentPlanRepository,
                             TradeRepository tradeRepository) {
        this.planActionRepository = planActionRepository;
        this.investmentPlanRepository = investmentPlanRepository;
        this.tradeRepository = tradeRepository;
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

    @Transactional
    public PlanActionResponse createPlanAction(Long investmentPlanId, PlanActionCreateRequest request) {
        InvestmentPlan investmentPlan = investmentPlanRepository.findByIdForUpdate(investmentPlanId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVESTMENT_PLAN_NOT_FOUND));

        investmentPlan.requireDraft();
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

    @Transactional(readOnly = true)
    public List<PlanActionResponse> getPlanActions(Long investmentPlanId) {
        return planActionRepository.findByInvestmentPlanId(investmentPlanId)
                .stream()
                .map(PlanActionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanActionResponse getPlanAction(Long planId, Long actionId) {
        PlanAction planAction = findActionInPlan(planId, actionId);

        return PlanActionResponse.from(planAction);
    }

    @Transactional
    public PlanActionResponse updatePlanAction(Long planId, Long actionId, PlanActionUpdateRequest request) {
        requireDraft(planId);
        lockAction(actionId);
        PlanAction planAction = findActionInPlan(planId, actionId);

        boolean coreChanged = planAction.getActionType() != request.actionType()
                || !Objects.equals(planAction.getTriggerPrice(), request.triggerPrice())
                || !Objects.equals(planAction.getQuantity(), request.quantity());
        if (coreChanged) validateUnexecuted(planAction);

        planAction.update(
                request.actionType(),
                request.triggerPrice(),
                request.quantity(),
                request.memo()
        );
        planAction.updateExecutionStatus(tradeRepository.sumQuantityByPlanActionId(planAction.getId()));

        return PlanActionResponse.from(planAction);
    }

    @Transactional
    public void deletePlanAction(Long planId, Long actionId) {
        requireDraft(planId);
        lockAction(actionId);
        PlanAction actionInPlan = findActionInPlan(planId, actionId);
        validateUnexecuted(actionInPlan);
        planActionRepository.delete(actionInPlan);
    }

    private void lockAction(Long id) {
        planActionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PLAN_ACTION_NOT_FOUND));
    }

    private void requireDraft(Long planId) {
        investmentPlanRepository.findByIdForUpdate(planId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVESTMENT_PLAN_NOT_FOUND)).requireDraft();
    }

    private void validateUnexecuted(PlanAction action) {
        if ((action.getSimulation() != null && action.getSimulation().getStatus() == SimulationStatus.EXECUTED)
                || tradeRepository.existsByPlanActionId(action.getId())) {
            throw new BadRequestException("실제 거래가 연결되었거나 가상 체결된 액션의 조건 수정 및 삭제는 불가능합니다.");
        }
    }

}
