package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.TradeCreateRequest;
import com.project.investment_tracker.dto.TradeUpdateRequest;
import com.project.investment_tracker.dto.PlanActionUpdateRequest;
import com.project.investment_tracker.entity.*;
import com.project.investment_tracker.global.error.BadRequestException;
import com.project.investment_tracker.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:action_status",
        "kis.app-key=test", "kis.app-secret=test", "kis.base-url=http://localhost"
})
class TradePlanActionStatusTests {
    @Autowired TradeService service;
    @Autowired PlanActionService actionService;
    @Autowired AccountRepository accounts;
    @Autowired TradeRepository trades;
    @Autowired StockHoldingRepository holdings;
    @Autowired InvestmentPlanRepository plans;
    @Autowired PlanActionRepository actions;

    private Long accountId;
    private InvestmentPlan plan;
    private final LocalDateTime time = LocalDateTime.of(2026, 1, 1, 12, 0);

    @BeforeEach
    void setUp() {
        trades.deleteAll();
        holdings.deleteAll();
        actions.deleteAll();
        plans.deleteAll();
        accounts.deleteAll();
        accountId = accounts.save(new Account("Test", 10000L)).getId();
        plan = plans.save(new InvestmentPlan("Test", "TEST", 10000L, "Reason"));
    }

    @Test
    @DisplayName("유일한 연결 거래를 삭제하면 액션이 대기 상태로 복원된다")
    void deletingLastLinkRestoresPending() {
        Long action = action();
        Long trade = buy(action, 0);
        assertStatus(action, ActionStatus.IN_PROGRESS);
        service.deleteTrade(trade);
        assertStatus(action, ActionStatus.PENDING);
        assertFalse(trades.existsById(trade));
        assertEquals(10000L, accounts.findById(accountId).orElseThrow().getCashBalance());
    }

    @Test
    @DisplayName("다른 연결 거래가 남아 있으면 삭제 후에도 진행 중 상태를 유지한다")
    void deletingOneOfMultipleLinksKeepsInProgress() {
        Long action = action();
        buy(action, 0);
        Long latest = buy(action, 1);
        service.deleteTrade(latest);
        assertStatus(action, ActionStatus.IN_PROGRESS);
        assertEquals(1, trades.count());
    }

    @Test
    @DisplayName("유일한 거래의 액션 연결을 해제하면 대기 상태로 복원된다")
    void detachingLastLinkRestoresPending() {
        Long action = action();
        Long trade = buy(action, 0);
        service.updateTrade(trade, update(null, 0, 1));
        assertStatus(action, ActionStatus.PENDING);
        assertNull(trades.findById(trade).orElseThrow().getPlanAction());
        assertEquals(9900L, accounts.findById(accountId).orElseThrow().getCashBalance());
    }

    @Test
    @DisplayName("다른 연결 거래가 있으면 연결 해제 후에도 진행 중 상태를 유지한다")
    void detachingOneOfMultipleLinksKeepsInProgress() {
        Long action = action();
        buy(action, 0);
        Long latest = buy(action, 1);
        service.updateTrade(latest, update(null, 1, 1));
        assertStatus(action, ActionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("액션을 교체하면 이전 액션은 대기 상태이고 새 액션은 진행 중 상태가 된다")
    void switchingActionsUpdatesBothStatuses() {
        Long oldAction = action();
        Long newAction = action();
        Long trade = buy(oldAction, 0);
        var response = service.updateTrade(trade, update(newAction, 0, 1));
        assertStatus(oldAction, ActionStatus.PENDING);
        assertStatus(newAction, ActionStatus.IN_PROGRESS);
        assertEquals(newAction, response.planActionId());
    }

    @Test
    @DisplayName("액션 교체 후에도 이전 액션에 다른 거래가 남으면 진행 중 상태를 유지한다")
    void switchingOneOfMultipleLinksKeepsOldActionInProgress() {
        Long oldAction = action();
        Long newAction = action();
        buy(oldAction, 0);
        Long latest = buy(oldAction, 1);
        service.updateTrade(latest, update(newAction, 1, 1));
        assertStatus(oldAction, ActionStatus.IN_PROGRESS);
        assertStatus(newAction, ActionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("액션 없는 거래에 연결을 추가하면 진행 중 상태가 된다")
    void attachingActionMarksInProgress() {
        Long trade = buy(null, 0);
        Long action = action();
        service.updateTrade(trade, update(action, 0, 1));
        assertStatus(action, ActionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("같은 액션을 유지하며 수량을 수정해도 진행 중 상태가 유지된다")
    void retainingActionKeepsInProgress() {
        Long action = action();
        Long trade = buy(action, 0);
        service.updateTrade(trade, update(action, 0, 2));
        assertStatus(action, ActionStatus.IN_PROGRESS);
        assertEquals(2, trades.findById(trade).orElseThrow().getQuantity());
    }

    @Test
    @DisplayName("액션 교체 중 잔액 부족으로 실패하면 연결과 양쪽 상태 및 잔액이 유지된다")
    void failedUpdatePreservesLinksAndStatuses() {
        Long oldAction = action();
        Long newAction = action();
        Long trade = buy(oldAction, 0);
        assertThrows(BadRequestException.class,
                () -> service.updateTrade(trade, update(newAction, 0, 101)));
        assertStatus(oldAction, ActionStatus.IN_PROGRESS);
        assertStatus(newAction, ActionStatus.PENDING);
        assertEquals(oldAction, service.getTrade(trade).planActionId());
        assertEquals(9900L, accounts.findById(accountId).orElseThrow().getCashBalance());
    }

    @Test
    @DisplayName("후속 거래 때문에 삭제가 거절되면 기존 진행 중 상태가 유지된다")
    void failedDeletionKeepsInProgress() {
        Long action = action();
        Long first = buy(action, 0);
        buy(null, 1);
        assertThrows(BadRequestException.class, () -> service.deleteTrade(first));
        assertStatus(action, ActionStatus.IN_PROGRESS);
        assertTrue(trades.existsById(first));
    }

    @Test
    @DisplayName("분할 거래 수량의 합계로 대기, 진행 중, 완료를 판단하고 삭제 시 복원한다")
    void partialTradesCompleteAndDeletionRestoresProgress() {
        Long action = action();
        assertStatus(action, ActionStatus.PENDING);
        Long first = buy(action, 0);
        service.updateTrade(first, update(action, 0, 3));
        assertStatus(action, ActionStatus.IN_PROGRESS);
        Long second = buy(action, 1);
        service.updateTrade(second, update(action, 1, 7));
        assertStatus(action, ActionStatus.EXECUTED);
        service.deleteTrade(second);
        assertStatus(action, ActionStatus.IN_PROGRESS);
        service.deleteTrade(first);
        assertStatus(action, ActionStatus.PENDING);
    }

    @Test
    @DisplayName("목표를 초과한 거래도 완료이고 거래 수량을 줄이면 진행 중으로 변경된다")
    void exceedingTargetCompletesAndReducingQuantityRestoresProgress() {
        Long action = action();
        Long trade = buy(action, 0);
        service.updateTrade(trade, update(action, 0, 11));
        assertStatus(action, ActionStatus.EXECUTED);
        service.updateTrade(trade, update(action, 0, 9));
        assertStatus(action, ActionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("실제 거래가 연결된 액션은 목표 수량을 변경할 수 없다")
    void changingExecutedTargetIsRejected() {
        Long action = action();
        buy(action, 0);
        assertThrows(com.project.investment_tracker.global.error.BadRequestException.class,
                () -> actionService.updatePlanAction(plan.getId(), action,
                        new PlanActionUpdateRequest(ActionType.BUY, 100L, 1, null)));
        assertStatus(action, ActionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("매도 액션도 연결된 매도 수량 합계로 완료 여부를 판단한다")
    void sellActionUsesSoldQuantity() {
        Long purchase = buy(null, 0);
        service.updateTrade(purchase, update(null, 0, 10));
        Long action = actions.save(new PlanAction(plan, ActionType.SELL, 100L, 10, null)).getId();
        service.createTrade(new TradeCreateRequest(accountId, "Test", "TEST", TradeType.SELL,
                100L, 3, time.plusDays(1), null, action));
        assertStatus(action, ActionStatus.IN_PROGRESS);
        service.createTrade(new TradeCreateRequest(accountId, "Test", "TEST", TradeType.SELL,
                100L, 7, time.plusDays(2), null, action));
        assertStatus(action, ActionStatus.EXECUTED);
    }

    private Long action() {
        return actions.save(new PlanAction(plan, ActionType.BUY, 100L, 10, null)).getId();
    }

    private Long buy(Long actionId, int day) {
        return service.createTrade(new TradeCreateRequest(accountId, "Test", "TEST", TradeType.BUY,
                100L, 1, time.plusDays(day), null, actionId)).id();
    }

    private TradeUpdateRequest update(Long actionId, int day, int quantity) {
        return new TradeUpdateRequest(TradeType.BUY, 100L, quantity, time.plusDays(day), null, actionId);
    }

    private void assertStatus(Long id, ActionStatus status) {
        assertEquals(status, actions.findById(id).orElseThrow().getActionStatus());
    }
}
