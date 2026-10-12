package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.StockDailyPriceResponse;
import com.project.investment_tracker.entity.*;
import com.project.investment_tracker.global.error.BadRequestException;
import com.project.investment_tracker.global.error.ResourceNotFoundException;
import com.project.investment_tracker.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class PlanSimulationServiceTests {
    @Autowired PlanSimulationService service;
    @Autowired InvestmentPlanRepository plans;
    @Autowired PlanActionRepository actions;
    @Autowired AccountRepository accounts;
    @Autowired StockHoldingRepository holdings;
    @Autowired PlanActionSimulationRepository simulationRepository;
    @Autowired TradeRepository trades;
    @Autowired PlatformTransactionManager manager;
    @Autowired EntityManager em;
    @MockitoBean StockDailyPriceService prices;
    @MockitoSpyBean PlanActionSimulationService simulations;

    private final LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
    private final List<Long> planIds = new ArrayList<>();
    private final List<Long> accountIds = new ArrayList<>();
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(manager);
    }

    // 서비스의 실제 커밋과 롤백을 검증하기 위해 테스트 전체를 트랜잭션으로 감싸지 않는다.
    @AfterEach
    void cleanUp() {
        transaction.executeWithoutResult(status -> {
            for (Long planId : planIds) {
                actions.deleteAll(actions.findByInvestmentPlanId(planId));
                plans.deleteById(planId);
            }
            for (Long accountId : accountIds) {
                holdings.deleteAll(holdings.findByAccountId(accountId));
                accounts.deleteById(accountId);
            }
        });
    }

    @Test
    @DisplayName("체결 기록으로 자산을 복원하고 평가가 끝난 기간은 다시 조회하지 않는다")
    void replaysFromSnapshotAndPreservesActualAssets() {
        var fixture = plan(today.minusDays(5), today.plusDays(5), 1000000L);
        stubPrices(List.of(bar(today.minusDays(4), 150000), bar(today.minusDays(2), 200000),
                bar(today.minusDays(1), 210000)));
        long tradeCount = trades.count();

        var result = service.simulate(fixture.planId());
        assertEquals(3250000L, result.state().cash());
        assertEquals(0, result.state().quantity());
        assertEquals(1250000L, result.state().realizedProfit());
        assertEquals(2, result.executions().size());
        assertTrue(result.skippedActions().isEmpty());
        assertEquals(today.minusDays(1), result.state().processedThrough());
        var repeated = service.simulate(fixture.planId());
        assertEquals(result.state(), repeated.state());
        assertTrue(repeated.executions().isEmpty());
        assertTrue(repeated.skippedActions().isEmpty());
        assertEvaluatedThrough(fixture.planId(), today.minusDays(1));
        assertExecuted(fixture.buyId(), today.minusDays(4));
        assertExecuted(fixture.sellId(), today.minusDays(2));
        long executedCount = transaction.execute(status -> actions.findByInvestmentPlanId(fixture.planId()).stream()
                .filter(action -> action.getSimulation().getStatus() == SimulationStatus.EXECUTED).count());
        assertEquals(2L, executedCount);
        assertEquals(9000000L, accounts.findById(fixture.accountId()).orElseThrow().getCashBalance());
        var holding = holdings.findById(fixture.holdingId()).orElseThrow();
        assertEquals(2, holding.getQuantity());
        assertEquals(180000L, holding.getTotalInvestmentAmount());
        assertEquals(tradeCount, trades.count());
        transaction.executeWithoutResult(status -> actions.findByInvestmentPlanId(fixture.planId())
                .forEach(action -> assertEquals(ActionStatus.PENDING, action.getActionStatus())));
        verify(prices).getDailyPrices("005930", today.minusDays(4), today.minusDays(1));
        verifyNoMoreInteractions(prices);
    }

    @Test
    @DisplayName("작성 중이거나 초기 상태가 없는 계획과 없는 계획은 시세 조회 전에 거절한다")
    void rejectsPlansWithoutBaseline() {
        Long draftId = transaction.execute(status -> {
            var account = accounts.save(new Account("Draft", 0L));
            accountIds.add(account.getId());
            var draft = plans.save(InvestmentPlan.draft(account, "삼성전자", "005930", 1000L, "",
                    today.plusDays(5), today.atStartOfDay()));
            planIds.add(draft.getId());
            return draft.getId();
        });
        var legacy = plans.save(new InvestmentPlan("삼성전자", "005930", 1000L, ""));
        planIds.add(legacy.getId());

        assertThrows(BadRequestException.class, () -> service.simulate(draftId));
        assertThrows(BadRequestException.class, () -> service.simulate(legacy.getId()));
        assertThrows(ResourceNotFoundException.class, () -> service.simulate(-1L));
        verifyNoInteractions(prices);
    }

    @Test
    @DisplayName("시작 당일은 시세를 요청하지 않고 초기 가상 자산을 반환한다")
    void returnsInitialStateOnStartDay() {
        var fixture = plan(today, today.plusDays(5), 1000000L);
        var result = service.simulate(fixture.planId());
        assertEquals(1000000L, result.state().cash());
        assertEquals(10, result.state().quantity());
        assertEquals(today, result.state().processedThrough());
        assertTrue(result.executions().isEmpty());
        assertPending(fixture.buyId());
        verifyNoInteractions(prices);
    }

    @Test
    @DisplayName("만료된 계획은 종료일까지만 조회하고 시작 당일과 종료일 이후 일봉을 제외한다")
    void limitsExpiredPlanPeriod() {
        var fixture = plan(today.minusDays(10), today.minusDays(5), 1000000L);
        stubPrices(List.of(bar(today.minusDays(10), 150000), bar(today.minusDays(5), 150000),
                bar(today.minusDays(4), 200000)));
        var result = service.simulate(fixture.planId());
        assertEquals(1, result.executions().size());
        assertExecuted(fixture.buyId(), today.minusDays(5));
        assertPending(fixture.sellId());
        verify(prices).getDailyPrices("005930", today.minusDays(9), today.minusDays(5));
    }

    @Test
    @DisplayName("시세 조회는 트랜잭션 밖에서 수행하며 조회 중 중도 종료된 계획의 종료일도 다시 확인한다")
    void rechecksEndDateAfterFetchingPrices() {
        var fixture = plan(today.minusDays(5), today.plusDays(5), 1000000L);
        when(prices.getDailyPrices(anyString(), any(), any())).thenAnswer(invocation -> {
            assertFalse(TransactionSynchronizationManager.isActualTransactionActive());
            transaction.executeWithoutResult(status -> plans.findByIdForUpdate(fixture.planId()).orElseThrow()
                    .close(today.minusDays(3).atTime(12, 0)));
            return List.of(bar(today.minusDays(3), 150000), bar(today.minusDays(2), 200000));
        });
        var result = service.simulate(fixture.planId());
        assertEquals(1, result.executions().size());
        assertExecuted(fixture.buyId(), today.minusDays(3));
        assertPending(fixture.sellId());
        assertEquals(today.minusDays(3), result.state().processedThrough());
    }

    @Test
    @DisplayName("현금과 수량 부족 내역은 반환하되 가상 체결 상태는 미체결로 유지한다")
    void keepsSkippedActionsPending() {
        var fixture = plan(today.minusDays(5), today.plusDays(5), 100L);
        stubPrices(List.of(bar(today.minusDays(4), 150000), bar(today.minusDays(3), 200000)));
        var result = service.simulate(fixture.planId());
        assertTrue(result.executions().isEmpty());
        assertEquals(List.of(VirtualExecutionCalculator.SkipReason.INSUFFICIENT_CASH,
                        VirtualExecutionCalculator.SkipReason.INSUFFICIENT_QUANTITY),
                result.skippedActions().stream().map(VirtualExecutionCalculator.SkippedAction::reason).toList());
        assertPending(fixture.buyId());
        assertPending(fixture.sellId());
        assertEvaluatedThrough(fixture.planId(), today.minusDays(3));
    }

    @Test
    @DisplayName("일봉이 없으면 초기 상태를 유지하고 시세 요청 실패 시 체결 기록을 변경하지 않는다")
    void handlesEmptyPricesAndFetchFailure() {
        var fixture = plan(today.minusDays(5), today.plusDays(5), 1000000L);
        when(prices.getDailyPrices(anyString(), any(), any()))
                .thenReturn(List.of()).thenThrow(new IllegalStateException("시세 조회 실패"));
        var result = service.simulate(fixture.planId());
        assertEquals(today.minusDays(5), result.state().processedThrough());
        assertTrue(result.executions().isEmpty());
        assertThrows(IllegalStateException.class, () -> service.simulate(fixture.planId()));
        assertPending(fixture.buyId());
        assertPending(fixture.sellId());
        assertEvaluatedThrough(fixture.planId(), null);
    }

    @Test
    @DisplayName("두 번째 체결 저장이 실패하면 먼저 저장한 체결도 함께 롤백한다")
    void rollsBackAllExecutionsOnFailure() {
        var fixture = plan(today.minusDays(5), today.plusDays(5), 1000000L);
        stubPrices(List.of(bar(today.minusDays(4), 150000), bar(today.minusDays(2), 200000)));
        doAnswer(invocation -> {
            em.flush();
            throw new IllegalStateException("체결 저장 실패");
        }).when(simulations).recordExecution(fixture.planId(), fixture.sellId(), today.minusDays(2));
        assertThrows(IllegalStateException.class, () -> service.simulate(fixture.planId()));
        assertPending(fixture.buyId());
        assertPending(fixture.sellId());
        assertEvaluatedThrough(fixture.planId(), null);
    }

    @Test
    @DisplayName("평가일 도입 전의 확정 체결은 일봉 재검증 없이 반영하고 다음 날부터 미체결만 평가한다")
    void restoresLegacyExecutionWithoutRecheckingPrices() {
        var fixture = plan(today.minusDays(5), today.plusDays(5), 1000000L);
        simulations.recordExecution(fixture.planId(), fixture.buyId(), today.minusDays(3));
        stubPrices(List.of(bar(today.minusDays(2), 200000)));
        var result = service.simulate(fixture.planId());
        assertEquals(3250000L, result.state().cash());
        assertEquals(1250000L, result.state().realizedProfit());
        assertEquals(List.of(fixture.sellId()), result.executions().stream()
                .map(VirtualExecutionCalculator.Execution::actionId).toList());
        assertExecuted(fixture.buyId(), today.minusDays(3));
        assertExecuted(fixture.sellId(), today.minusDays(2));
        assertEvaluatedThrough(fixture.planId(), today.minusDays(2));
        verify(prices).getDailyPrices("005930", today.minusDays(2), today.minusDays(1));
    }

    @Test
    @DisplayName("동일 계획을 동시에 계산해도 체결 기록이 중복되지 않는다")
    void serializesConcurrentSimulations() throws Exception {
        var fixture = plan(today.minusDays(5), today.plusDays(5), 1000000L);
        var ready = new CountDownLatch(2);
        when(prices.getDailyPrices(anyString(), any(), any())).thenAnswer(invocation -> {
            ready.countDown();
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            return List.of(bar(today.minusDays(4), 150000), bar(today.minusDays(2), 200000));
        });
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> service.simulate(fixture.planId()));
            var second = executor.submit(() -> service.simulate(fixture.planId()));
            var firstResult = first.get(15, TimeUnit.SECONDS);
            var secondResult = second.get(15, TimeUnit.SECONDS);
            assertEquals(firstResult.state(), secondResult.state());
            assertEquals(2, firstResult.executions().size() + secondResult.executions().size());
            assertEvaluatedThrough(fixture.planId(), today.minusDays(2));
            assertExecuted(fixture.buyId(), today.minusDays(4));
            assertExecuted(fixture.sellId(), today.minusDays(2));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    @DisplayName("1년을 넘는 계획은 일봉 서비스의 기간 제한에 맞춰 분할 조회한다")
    void splitsLongHistoryRequests() {
        LocalDate start = today.minusYears(2).minusDays(5);
        var fixture = plan(start, today.plusDays(5), 1000000L);
        stubPrices(List.of());
        service.simulate(fixture.planId());
        LocalDate from = start.plusDays(1);
        verify(prices).getDailyPrices("005930", from, from.plusYears(1));
        from = from.plusYears(1).plusDays(1);
        verify(prices).getDailyPrices("005930", from, from.plusYears(1));
        from = from.plusYears(1).plusDays(1);
        verify(prices).getDailyPrices("005930", from, today.minusDays(1));
        verifyNoMoreInteractions(prices);
    }

    @Test
    @DisplayName("체결이 없는 날도 평가일을 저장하고 다음 실행에서는 이후 일봉만 조회한다")
    void advancesCheckpointWithoutExecutions() {
        var fixture = plan(today.minusDays(5), today.plusDays(5), 1000000L);
        when(prices.getDailyPrices("005930", today.minusDays(4), today.minusDays(1)))
                .thenReturn(List.of(bar(today.minusDays(3), 100000)));
        when(prices.getDailyPrices("005930", today.minusDays(2), today.minusDays(1)))
                .thenReturn(List.of(bar(today.minusDays(2), 150000), bar(today.minusDays(1), 200000)));
        var first = service.simulate(fixture.planId());
        assertTrue(first.executions().isEmpty());
        assertEvaluatedThrough(fixture.planId(), today.minusDays(3));
        var second = service.simulate(fixture.planId());
        assertEquals(2, second.executions().size());
        assertEquals(1250000L, second.state().realizedProfit());
        assertEvaluatedThrough(fixture.planId(), today.minusDays(1));
        verify(prices).getDailyPrices("005930", today.minusDays(4), today.minusDays(1));
        verify(prices).getDailyPrices("005930", today.minusDays(2), today.minusDays(1));
        verifyNoMoreInteractions(prices);
    }

    @Test
    @DisplayName("저장된 매수의 자산을 복원하여 새 매도를 체결하고 과거 일봉은 무시한다")
    void resumesWithConfirmedPosition() {
        var fixture = plan(today.minusDays(5), today.plusDays(5), 1000000L);
        when(prices.getDailyPrices(anyString(), any(), any()))
                .thenReturn(List.of(bar(today.minusDays(4), 150000), bar(today.minusDays(3), 170000)))
                .thenReturn(List.of(bar(today.minusDays(4), 200000), bar(today.minusDays(1), 200000)));
        var first = service.simulate(fixture.planId());
        assertEquals(15, first.state().quantity());
        assertEvaluatedThrough(fixture.planId(), today.minusDays(3));
        var second = service.simulate(fixture.planId());
        assertEquals(1, second.executions().size());
        assertEquals(1250000L, second.state().realizedProfit());
        assertExecuted(fixture.buyId(), today.minusDays(4));
        assertExecuted(fixture.sellId(), today.minusDays(1));
        verify(prices).getDailyPrices("005930", today.minusDays(2), today.minusDays(1));
    }

    @Test
    @DisplayName("추가 기간 처리 실패 시 이전 체결과 평가일을 유지하고 같은 기간을 재시도한다")
    void preservesCheckpointAndRetriesFailedPeriod() {
        var fixture = plan(today.minusDays(5), today.plusDays(5), 1000000L);
        when(prices.getDailyPrices(anyString(), any(), any()))
                .thenReturn(List.of(bar(today.minusDays(4), 150000)))
                .thenThrow(new IllegalStateException("시세 조회 실패"))
                .thenReturn(List.of(bar(today.minusDays(1), 200000)));
        service.simulate(fixture.planId());
        assertThrows(IllegalStateException.class, () -> service.simulate(fixture.planId()));
        assertEvaluatedThrough(fixture.planId(), today.minusDays(4));
        assertExecuted(fixture.buyId(), today.minusDays(4));
        assertPending(fixture.sellId());
        service.simulate(fixture.planId());
        assertEvaluatedThrough(fixture.planId(), today.minusDays(1));
        verify(prices, times(2)).getDailyPrices("005930", today.minusDays(3), today.minusDays(1));
    }

    private Fixture plan(LocalDate start, LocalDate end, long cash) {
        return transaction.execute(status -> {
            // 현재 계좌 자산과 계획 시작 당시 스냅샷이 달라도 스냅샷으로 계산해야 한다.
            var account = accounts.save(new Account("Simulation", 9000000L));
            accountIds.add(account.getId());
            var holding = holdings.save(new StockHolding(account, "삼성전자", "005930", 2, 90000L));
            var plan = plans.save(new InvestmentPlan(account, "삼성전자", "005930", cash, "test",
                    10, 1000000L, 100000L, start.atStartOfDay(), end));
            planIds.add(plan.getId());
            var buy = actions.save(new PlanAction(plan, ActionType.BUY, 150000L, 5, ""));
            var sell = actions.save(new PlanAction(plan, ActionType.SELL, 200000L, 15, ""));
            return new Fixture(plan.getId(), account.getId(), holding.getId(), buy.getId(), sell.getId());
        });
    }

    private StockDailyPriceResponse bar(LocalDate date, long price) {
        return new StockDailyPriceResponse("005930", date, price, price, price, price);
    }

    private void assertEvaluatedThrough(Long planId, LocalDate expected) {
        assertEquals(expected, plans.findById(planId).orElseThrow().getSimulationEvaluatedThrough());
    }

    private void stubPrices(List<StockDailyPriceResponse> dailyPrices) {
        when(prices.getDailyPrices(anyString(), any(), any())).thenReturn(dailyPrices);
    }

    private void assertPending(Long actionId) {
        var simulation = simulationRepository.findByPlanActionId(actionId).orElseThrow();
        assertEquals(SimulationStatus.PENDING, simulation.getStatus());
        assertNull(simulation.getExecutedDate());
    }

    private void assertExecuted(Long actionId, LocalDate date) {
        var simulation = simulationRepository.findByPlanActionId(actionId).orElseThrow();
        assertEquals(SimulationStatus.EXECUTED, simulation.getStatus());
        assertEquals(date, simulation.getExecutedDate());
    }

    private record Fixture(Long planId, Long accountId, Long holdingId, Long buyId, Long sellId) {}
}
