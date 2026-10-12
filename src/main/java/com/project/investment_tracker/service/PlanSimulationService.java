package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.StockDailyPriceResponse;
import com.project.investment_tracker.entity.InvestmentPlan;
import com.project.investment_tracker.entity.PlanAction;
import com.project.investment_tracker.entity.PlanStatus;
import com.project.investment_tracker.entity.SimulationStatus;
import com.project.investment_tracker.global.error.BadRequestException;
import com.project.investment_tracker.global.error.ErrorMessage;
import com.project.investment_tracker.global.error.ResourceNotFoundException;
import com.project.investment_tracker.repository.InvestmentPlanRepository;
import com.project.investment_tracker.repository.PlanActionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class PlanSimulationService {
    private final InvestmentPlanRepository plans;
    private final PlanActionRepository actions;
    private final StockDailyPriceService prices;
    private final VirtualExecutionCalculator calculator;
    private final PlanActionSimulationService simulations;
    private final TransactionTemplate transaction;

    public PlanSimulationService(InvestmentPlanRepository plans, PlanActionRepository actions,
                                 StockDailyPriceService prices, VirtualExecutionCalculator calculator,
                                 PlanActionSimulationService simulations, PlatformTransactionManager manager) {
        this.plans = plans;
        this.actions = actions;
        this.prices = prices;
        this.calculator = calculator;
        this.simulations = simulations;
        this.transaction = new TransactionTemplate(manager);
    }

    // 외부 시세 호출은 트랜잭션 밖에서 수행하고, 조회와 결과 저장에만 짧은 트랜잭션을 사용한다.
    // 반환 상태는 누적 자산이며, 체결/스킵 목록은 이번에 새로 평가한 기간의 내역이다.
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public VirtualExecutionCalculator.Result simulate(Long planId) {
        LocalDate yesterday = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        var input = transaction.execute(status -> prepareInput(findPlan(planId), yesterday));
        var dailyPrices = loadDailyPrices(input);
        return transaction.execute(status -> calculateAndRecord(planId, input.throughDate(), dailyPrices));
    }

    private InvestmentPlan findPlan(Long planId) {
        // 평가일과 체결 기록을 읽는 사이에 다른 시뮬레이션이 결과를 저장하지 않도록 한다.
        return plans.findByIdForUpdate(planId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVESTMENT_PLAN_NOT_FOUND));
    }

    private SimulationInput prepareInput(InvestmentPlan plan, LocalDate latestDate) {
        if (plan.getPlanStatus() == PlanStatus.DRAFT) {
            throw new BadRequestException("시작한 계획만 시뮬레이션할 수 있습니다.");
        }
        if (plan.getStartedAt() == null || plan.getInitialCash() == null
                || plan.getInitialQuantity() == null || plan.getInitialCostBasis() == null
                || plan.getPlannedEndDate() == null) {
            throw new BadRequestException("시작 당시 초기 상태가 없는 계획은 시뮬레이션할 수 없습니다.");
        }
        LocalDate startedDate = plan.getStartedAt().toLocalDate();
        LocalDate endDate = plan.getEndedAt() == null
                ? plan.getPlannedEndDate() : plan.getEndedAt().toLocalDate();
        LocalDate throughDate = endDate.isBefore(latestDate) ? endDate : latestDate;
        // 시작 당일에는 평가할 일봉이 없으므로 초기 상태만 반환한다.
        if (throughDate.isBefore(startedDate)) throughDate = startedDate;
        var initial = VirtualExecutionCalculator.State.initial(plan.getInitialCash(), plan.getInitialQuantity(),
                plan.getInitialCostBasis(), startedDate);
        var planActions = actions.findByInvestmentPlanId(plan.getId());
        var confirmed = planActions.stream().filter(this::isExecuted)
                .map(action -> new VirtualExecutionCalculator.ConfirmedExecution(toCalculatorAction(action),
                        action.getSimulation().getExecutedDate())).toList();
        LocalDate evaluatedThrough = resolveEvaluatedThrough(plan, confirmed);
        var restored = calculator.restore(initial, confirmed, evaluatedThrough);
        var pending = planActions.stream().filter(action -> !isExecuted(action))
                .map(this::toCalculatorAction).toList();
        return new SimulationInput(plan.getStockSymbol(), restored, pending, throughDate);
    }

    private boolean isExecuted(PlanAction action) {
        return action.getSimulation() != null && action.getSimulation().getStatus() == SimulationStatus.EXECUTED;
    }

    private VirtualExecutionCalculator.Action toCalculatorAction(PlanAction action) {
        return new VirtualExecutionCalculator.Action(action.getId(), action.getActionType(),
                action.getTriggerPrice(), action.getQuantity());
    }

    private LocalDate resolveEvaluatedThrough(InvestmentPlan plan,
                                             List<VirtualExecutionCalculator.ConfirmedExecution> confirmed) {
        if (plan.getSimulationEvaluatedThrough() != null) return plan.getSimulationEvaluatedThrough();
        // 평가일 필드 도입 전의 기록은 마지막 확정 체결일까지 보존하고 이후 날짜부터 평가한다.
        return confirmed.stream().map(VirtualExecutionCalculator.ConfirmedExecution::date)
                .max(LocalDate::compareTo).orElse(plan.getStartedAt().toLocalDate());
    }

    private List<StockDailyPriceResponse> loadDailyPrices(SimulationInput input) {
        var dailyPrices = new ArrayList<StockDailyPriceResponse>();
        LocalDate from = input.restored().processedThrough().plusDays(1);
        // 일봉 서비스의 요청당 최대 1년 제한에 맞춰 긴 계획도 나눠 조회한다.
        while (!from.isAfter(input.throughDate())) {
            LocalDate to = from.plusYears(1).isBefore(input.throughDate())
                    ? from.plusYears(1) : input.throughDate();
            dailyPrices.addAll(prices.getDailyPrices(input.symbol(), from, to));
            from = to.plusDays(1);
        }
        return dailyPrices;
    }

    private VirtualExecutionCalculator.Result calculateAndRecord(
            Long planId, LocalDate fetchedThrough, List<StockDailyPriceResponse> dailyPrices) {
        var plan = plans.findByIdForUpdate(planId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVESTMENT_PLAN_NOT_FOUND));
        // 시세 조회 중 계획이 종료되었을 수 있으므로 잠금 후 계산 기간을 다시 확인한다.
        var input = prepareInput(plan, fetchedThrough);
        // 다른 요청이 먼저 평가를 마쳤다면 해당 기간의 일봉을 다시 반영하지 않는다.
        if (!input.throughDate().isAfter(input.restored().processedThrough())) {
            return new VirtualExecutionCalculator.Result(input.restored(), List.of(), List.of());
        }
        var result = calculator.calculate(input.symbol(), input.restored(), input.pendingActions(),
                dailyPrices, input.throughDate());
        recordExecutions(planId, result.executions());
        if (result.state().processedThrough().isAfter(plan.getStartedAt().toLocalDate())) {
            plan.markSimulationEvaluatedThrough(result.state().processedThrough());
        }
        return result;
    }

    private void recordExecutions(Long planId, List<VirtualExecutionCalculator.Execution> executions) {
        for (var execution : executions) {
            simulations.recordExecution(planId, execution.actionId(), execution.date());
        }
    }

    private record SimulationInput(String symbol, VirtualExecutionCalculator.State restored,
                                   List<VirtualExecutionCalculator.Action> pendingActions, LocalDate throughDate) {}
}
