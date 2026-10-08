package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.*;
import com.project.investment_tracker.entity.*;
import com.project.investment_tracker.repository.*;
import com.project.investment_tracker.global.error.BadRequestException;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PlanActionSimulationTests {
    @Autowired PlanActionService actions;
    @Autowired PlanActionSimulationService simulations;
    @Autowired InvestmentPlanRepository plans;
    @Autowired AccountRepository accounts;
    @Autowired PlanActionRepository actionRepository;
    @Autowired PlanActionSimulationRepository simulationRepository;
    @Autowired TradeRepository trades;
    @Autowired EntityManager em;
    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    private InvestmentPlan plan() {
        var account = accounts.save(new Account("Simulation", 10000L));
        return plans.save(new InvestmentPlan(account, "삼성전자", "005930", 10000L, "test",
                0, 0, 100, today.minusDays(5).atStartOfDay(), today.plusDays(5)));
    }
    private PlanActionResponse action(InvestmentPlan plan) {
        return PlanActionResponse.from(actionRepository.save(new PlanAction(plan, ActionType.BUY, 100L, 10, "test")));
    }

    @Test @DisplayName("동일한 체결 요청은 안전하게 재처리하되, 기록을 임의로 바꾸거나 실제 거래 상태에 영향을 주면 안 된다")
    void persistsAndReplaysIdempotently() {
        var plan = plan();
        var action = action(plan);
        assertEquals(SimulationStatus.PENDING, action.simulationStatus());
        assertNull(action.executedDate());
        simulations.recordExecution(plan.getId(), action.id(), today.minusDays(1));
        simulations.recordExecution(plan.getId(), action.id(), today.minusDays(1));
        em.flush();
        em.clear();
        var result = actions.getPlanAction(plan.getId(), action.id());
        assertEquals(SimulationStatus.EXECUTED, result.simulationStatus());
        assertEquals(today.minusDays(1), result.executedDate());
        assertEquals(ActionStatus.PENDING, result.actionStatus());
        assertTrue(simulationRepository.findByPlanActionId(action.id()).isPresent());
        assertThrows(BadRequestException.class, () -> simulations.recordExecution(plan.getId(), action.id(), today.minusDays(2)));
    }

    @Test @DisplayName("시작 후에는 가상 체결 여부와 관계없이 조건과 메모를 고정한다")
    void executedActionsAreProtected() {
        var plan = plan();
        var action = action(plan);
        simulations.recordExecution(plan.getId(), action.id(), today.minusDays(1));
        assertThrows(BadRequestException.class, () -> actions.updatePlanAction(plan.getId(), action.id(),
                new PlanActionUpdateRequest(ActionType.BUY, 200L, 10, "changed")));
        assertThrows(BadRequestException.class, () -> actions.deletePlanAction(plan.getId(), action.id()));
        assertThrows(BadRequestException.class, () -> actions.updatePlanAction(plan.getId(), action.id(),
                new PlanActionUpdateRequest(ActionType.BUY, 100L, 10, "memo")));
    }

    @Test @DisplayName("미체결 액션 삭제 시 연결된 상태도 삭제한다")
    void pendingActionCanBeEditedAndDeleted() {
        var account = accounts.save(new Account("Draft", 10000L));
        var plan = plans.save(InvestmentPlan.draft(account, "삼성전자", "005930", 10000L, "test",
                today.plusDays(5), today.atStartOfDay()));
        var action = action(plan);
        actions.updatePlanAction(plan.getId(), action.id(), new PlanActionUpdateRequest(ActionType.BUY, 200L, 5, ""));
        actions.deletePlanAction(plan.getId(), action.id());
        em.flush();
        assertTrue(simulationRepository.findByPlanActionId(action.id()).isEmpty());
    }

    @Test @DisplayName("실제 거래가 연결되면 가상 미체결 상태라도 삭제하지 못한다")
    void actualTradeBlocksDeletion() {
        var plan = plan();
        var action = action(plan);
        trades.save(new Trade(plan.getAccount(), "삼성전자", "005930", TradeType.BUY, 100L, 1,
                today.minusDays(1).atStartOfDay(), "", actionRepository.findById(action.id()).orElseThrow()));
        assertThrows(BadRequestException.class, () -> actions.deletePlanAction(plan.getId(), action.id()));
    }

    @Test @DisplayName("체결일은 시작일 이후 완료된 날짜로 제한한다")
    void rejectsInvalidDates() {
        var plan = plan();
        var action = action(plan);
        assertThrows(BadRequestException.class, () -> simulations.recordExecution(plan.getId(), action.id(), today));
        assertThrows(BadRequestException.class, () -> simulations.recordExecution(plan.getId(), action.id(), today.minusDays(5)));
        assertThrows(BadRequestException.class, () -> simulations.recordExecution(plan.getId(), action.id(), null));
        assertEquals(SimulationStatus.PENDING, actions.getPlanAction(plan.getId(), action.id()).simulationStatus());
    }

    @Test @DisplayName("기존 액션의 누락 상태 초기화는 반복해도 하나만 생성한다")
    void initializesLegacyRowsOnce() {
        var plan = plan();
        var action = action(plan);
        em.flush();
        em.createNativeQuery("delete from plan_action_simulation where plan_action_id = :id")
                .setParameter("id", action.id()).executeUpdate();
        em.clear();
        assertTrue(actionRepository.findIdsWithoutSimulation().contains(action.id()));
        simulations.initializePending(action.id());
        simulations.initializePending(action.id());
        em.flush();
        em.clear();
        assertEquals(SimulationStatus.PENDING, simulationRepository.findByPlanActionId(action.id()).orElseThrow().getStatus());
        assertFalse(actionRepository.findIdsWithoutSimulation().contains(action.id()));
    }
}
