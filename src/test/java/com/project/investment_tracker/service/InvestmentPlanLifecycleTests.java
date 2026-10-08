package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.InvestmentPlanCreateRequest;
import com.project.investment_tracker.dto.InvestmentPlanUpdateRequest;
import com.project.investment_tracker.dto.StockPriceResponse;
import com.project.investment_tracker.entity.*;
import com.project.investment_tracker.repository.*;
import com.project.investment_tracker.global.error.BadRequestException;
import com.project.investment_tracker.global.error.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
class InvestmentPlanLifecycleTests {
    @Autowired InvestmentPlanService service;
    @Autowired AccountRepository accounts;
    @Autowired StockHoldingRepository holdings;
    @Autowired InvestmentPlanRepository plans;
    @Autowired PlanActionService actions;
    @MockitoBean StockPriceService prices;
    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));

    private InvestmentPlanCreateRequest request(Long accountId) {
        return new InvestmentPlanCreateRequest(accountId, today.plusDays(10), "삼성전자", "005930", 1000000L, "test");
    }
    private Account account() {
        when(prices.getStockPrice("005930")).thenReturn(new StockPriceResponse("005930", 150000L));
        return accounts.save(new Account("Test", 2000000L));
    }

    private com.project.investment_tracker.dto.InvestmentPlanResponse ready(Long accountId) {
        var draft = service.createInvestmentPlan(request(accountId));
        actions.createPlanAction(draft.id(), new com.project.investment_tracker.dto.PlanActionCreateRequest(ActionType.BUY, 100L, 1, ""));
        return draft;
    }

    @Test @DisplayName("계획 생성은 계좌 잔액을 변경하지 않고, 이후 보유 상태가 바뀌어도 초기 상태를 보존한다")
    void preservesSnapshot() {
        var account = account();
        var holding = holdings.save(new StockHolding(account, "삼성전자", "005930", 10, 100000L));
        var draft = ready(account.getId());
        assertEquals(PlanStatus.DRAFT, draft.planStatus());
        assertNull(draft.startedAt());
        assertNull(draft.initialQuantity());
        verify(prices, never()).getStockPrice(anyString());
        holding.buy(100000L, 1);
        var plan = service.startPlan(draft.id());
        holding.buy(200000L, 5);
        assertEquals(11, service.getPlan(plan.id()).initialQuantity());
        assertEquals(1100000L, plan.initialCostBasis());
        assertEquals(150000L, plan.initialPrice());
        assertEquals(1000000L, plan.initialCash());
        assertEquals(account.getId(), plan.accountId());
        assertEquals(2000000L, account.getCashBalance());
        assertNotNull(plan.startedAt());
    }

    @Test @DisplayName("같은 계좌 종목은 활성 중과 종료 당일에 중복 생성할 수 없다")
    void blocksDuplicateAndSameDayRestart() {
        var account = account();
        var plan = service.startPlan(ready(account.getId()).id());
        var next = ready(account.getId());
        assertThrows(BadRequestException.class, () -> service.startPlan(next.id()));
        var closed = service.closePlan(plan.id());
        assertEquals(PlanStatus.CANCELLED, closed.planStatus());
        assertEquals(today, closed.endedAt().toLocalDate());
        assertThrows(BadRequestException.class, () -> service.startPlan(next.id()));
        assertThrows(BadRequestException.class, () -> service.closePlan(plan.id()));
    }

    @Test @DisplayName("기간 만료 다음 날에는 새 계획을 생성하며 기존 계획은 완료로 조회된다")
    void restartsAfterExpiredPlan() {
        var account = account();
        var old = plans.save(new InvestmentPlan(account, "삼성전자", "005930", 1000000L, "old",
                0, 0, 100000, today.minusDays(3).atStartOfDay(), today.minusDays(1)));
        assertEquals(PlanStatus.COMPLETED, service.getPlan(old.getId()).planStatus());
        assertEquals(today.minusDays(1), service.getPlan(old.getId()).endedAt().toLocalDate());
        assertEquals(0, service.startPlan(ready(account.getId()).id()).initialQuantity());
    }

    @Test @DisplayName("예정 종료일 당일에는 여전히 활성이고 다른 계좌는 같은 종목 계획을 생성할 수 있다")
    void endsAtDayBoundaryAndSeparatesAccounts() {
        var a = account();
        var b = account();
        plans.save(new InvestmentPlan(a, "삼성전자", "005930", 1000000L, "old",
                0, 0, 100000, today.minusDays(3).atStartOfDay(), today));
        var next = ready(a.getId());
        assertThrows(BadRequestException.class, () -> service.startPlan(next.id()));
        assertEquals(PlanStatus.ACTIVE, service.startPlan(ready(b.getId()).id()).planStatus());
    }

    @Test @DisplayName("초안 편집을 허용하고 시작 이후에는 변경과 삭제를 막는다")
    void freezesBaseline() {
        var plan = ready(account().getId());
        assertEquals(2000000L, service.updatePlan(plan.id(),
                new InvestmentPlanUpdateRequest("삼성전자", "005930", 2000000L, "changed")).totalBudget());
        assertEquals("changed", service.updatePlan(plan.id(),
                new InvestmentPlanUpdateRequest("삼성전자", "005930", 1000000L, "changed")).reason());
        service.startPlan(plan.id());
        assertThrows(BadRequestException.class, () -> service.updatePlan(plan.id(),
                new InvestmentPlanUpdateRequest("삼성전자", "005930", 1000000L, "changed")));
        assertThrows(BadRequestException.class, () -> service.deletePlan(plan.id()));
    }

    @Test @DisplayName("액션 없는 초안과 만료된 초안은 시작할 수 없고 초안 삭제는 액션도 함께 지운다")
    void validatesStartAndDeletesDraft() {
        var account = account();
        var draft = service.createInvestmentPlan(request(account.getId()));
        assertThrows(BadRequestException.class, () -> service.startPlan(draft.id()));
        var expired = plans.save(InvestmentPlan.draft(account, "삼성전자", "005930", 1000L, "",
                today.minusDays(1), today.minusDays(3).atStartOfDay()));
        actions.createPlanAction(expired.getId(), new com.project.investment_tracker.dto.PlanActionCreateRequest(ActionType.BUY, 100L, 1, ""));
        assertThrows(BadRequestException.class, () -> service.startPlan(expired.getId()));
        service.deletePlan(expired.getId());
        assertFalse(plans.existsById(expired.getId()));
    }

    @Test @DisplayName("시작 후에는 미체결 액션도 추가 수정 삭제할 수 없다")
    void freezesAllActionsOnStart() {
        var draft = ready(account().getId());
        var action = actions.getPlanActions(draft.id()).get(0);
        service.startPlan(draft.id());
        assertThrows(BadRequestException.class, () -> actions.createPlanAction(draft.id(),
                new com.project.investment_tracker.dto.PlanActionCreateRequest(ActionType.SELL, 200L, 1, "")));
        assertThrows(BadRequestException.class, () -> actions.updatePlanAction(draft.id(), action.id(),
                new com.project.investment_tracker.dto.PlanActionUpdateRequest(ActionType.BUY, 200L, 1, "")));
        assertThrows(BadRequestException.class, () -> actions.deletePlanAction(draft.id(), action.id()));
    }

    @Test @DisplayName("없는 계좌와 잘못된 종료일은 시세 호출 전에 거절한다")
    void validatesBeforeFetchingQuote() {
        var account = accounts.save(new Account("Test", 0L));
        assertThrows(ResourceNotFoundException.class, () -> service.createInvestmentPlan(request(-1L)));
        assertThrows(BadRequestException.class, () -> service.createInvestmentPlan(
                new InvestmentPlanCreateRequest(account.getId(), today, "삼성전자", "005930", 1000L, "test")));
        verifyNoInteractions(prices);
    }
}
