package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.PlanActionResponse;
import com.project.investment_tracker.global.error.*;
import com.project.investment_tracker.repository.PlanActionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;

@Service
public class PlanActionSimulationService {
    private final PlanActionRepository actions;

    public PlanActionSimulationService(PlanActionRepository actions) { this.actions = actions; }

    @Transactional
    public void initializePending(Long actionId) {
        actions.findByIdForUpdate(actionId).ifPresent(action -> action.initializeSimulation());
    }

    // 시세와 자금/수량 검증을 마친 시뮬레이션 처리기가 호출할 내부 기록 기능이다.
    @Transactional
    public PlanActionResponse recordExecution(Long planId, Long actionId, LocalDate date) {
        var action = actions.findByIdForUpdate(actionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PLAN_ACTION_NOT_FOUND));
        var plan = action.getInvestmentPlan();
        if (!plan.getId().equals(planId)) throw new InvalidRelationException(ErrorMessage.PLAN_ACTION_NOT_BELONG_TO_PLAN);
        if (plan.getStartedAt() == null) throw new BadRequestException("초기 상태가 없는 기존 계획은 가상 체결할 수 없습니다.");
        var lastDate = plan.getEndedAt() == null ? plan.getPlannedEndDate() : plan.getEndedAt().toLocalDate();
        if (date == null || !date.isAfter(plan.getStartedAt().toLocalDate()) || date.isAfter(lastDate)
                || !date.isBefore(LocalDate.now(ZoneId.of("Asia/Seoul")))) {
            throw new BadRequestException("가상 체결일은 계획 시작일 이후의 완료된 날짜이며 종료일 이내여야 합니다.");
        }
        action.initializeSimulation().execute(date);
        return PlanActionResponse.from(action);
    }
}
