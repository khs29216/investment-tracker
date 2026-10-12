package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.StockDailyPriceResponse;
import com.project.investment_tracker.entity.ActionType;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;

@Component
public class VirtualExecutionCalculator {
    public record Action(long id, ActionType type, long targetPrice, int quantity) {}

    public record State(long cash, int quantity, long costBasis, long averagePrice,
                        long realizedProfit, LocalDate processedThrough, Set<Long> executedActionIds) {
        public State {
            Objects.requireNonNull(processedThrough);
            executedActionIds = Set.copyOf(executedActionIds);
            if (cash < 0 || quantity < 0 || costBasis < 0 || averagePrice < 0
                    || (quantity == 0 && (costBasis != 0 || averagePrice != 0))
                    || Math.multiplyExact(averagePrice, (long) quantity) > costBasis) {
                throw new IllegalArgumentException("가상 자산의 현금, 보유 수량 또는 원가가 올바르지 않습니다.");
            }
        }

        public static State initial(long cash, int quantity, long costBasis, LocalDate startedDate) {
            return new State(cash, quantity, costBasis, quantity == 0 ? 0 : costBasis / quantity,
                    0, startedDate, Set.of());
        }
    }

    public enum SkipReason { INSUFFICIENT_CASH, INSUFFICIENT_QUANTITY }
    public record SkippedAction(long actionId, LocalDate date, SkipReason reason) {}
    public record Execution(long actionId, LocalDate date, ActionType type, long price, int quantity,
                            long amount, long costBasis, long realizedProfit) {}
    public record Result(State state, List<Execution> executions, List<SkippedAction> skippedActions) {
        public Result {
            executions = List.copyOf(executions);
            skippedActions = List.copyOf(skippedActions);
        }
    }

    // throughDate까지 완료된 일봉을 전달한다. API 조회, 저장, 현재 시각 판단은 호출자 책임이다.
    public Result calculate(String symbol, State initial, List<Action> actions,
                            List<StockDailyPriceResponse> prices, LocalDate throughDate) {
        validateCalculationPeriod(symbol, initial, throughDate);
        var sortedActions = validateAndSortActions(actions);
        var bars = prepareDailyPrices(symbol, initial.processedThrough(), throughDate, prices);
        var calculation = new Calculation(initial);

        for (var bar : bars.values()) {
            for (var action : sortedActions) {
                executeAction(calculation, action, bar);
            }
        }

        return buildResult(calculation, initial.processedThrough(), bars);
    }

    private void validateCalculationPeriod(String symbol, State initial, LocalDate throughDate) {
        Objects.requireNonNull(symbol);
        Objects.requireNonNull(throughDate);
        if (throughDate.isBefore(initial.processedThrough())) {
            throw new IllegalArgumentException("마지막 처리일 이전으로 계산 종료일을 지정할 수 없습니다.");
        }
    }

    private List<Action> validateAndSortActions(List<Action> actions) {
        var sortedActions = actions.stream().sorted(Comparator.comparingLong(Action::id)).toList();
        Set<Long> actionIds = new HashSet<>();
        for (var action : sortedActions) {
            if (action.id() <= 0 || action.type() == null || action.targetPrice() <= 0
                    || action.quantity() <= 0 || !actionIds.add(action.id())) {
                throw new IllegalArgumentException("액션 정보가 올바르지 않거나 액션 ID가 중복되었습니다.");
            }
        }
        return sortedActions;
    }

    private NavigableMap<LocalDate, StockDailyPriceResponse> prepareDailyPrices(
            String symbol, LocalDate processedThrough, LocalDate throughDate,
            List<StockDailyPriceResponse> prices) {
        var bars = new TreeMap<LocalDate, StockDailyPriceResponse>();
        for (var price : prices) {
            if (!symbol.equals(price.stockSymbol()) || price.date() == null) {
                throw new IllegalArgumentException("일봉의 종목이 요청한 종목과 다르거나 날짜가 없습니다.");
            }
            if (!price.date().isAfter(processedThrough) || price.date().isAfter(throughDate)) continue;
            validateDailyPrice(price);
            var previous = bars.putIfAbsent(price.date(), price);
            if (previous != null && !previous.equals(price)) {
                throw new IllegalArgumentException("같은 날짜에 서로 다른 일봉 데이터가 존재합니다.");
            }
        }
        return bars;
    }

    private void validateDailyPrice(StockDailyPriceResponse price) {
        if (price.low() <= 0 || price.high() < price.low()
                || price.open() < price.low() || price.open() > price.high()
                || price.close() < price.low() || price.close() > price.high()) {
            throw new IllegalArgumentException("일봉의 시가, 고가, 저가 또는 종가가 올바르지 않습니다.");
        }
    }

    private void executeAction(Calculation calculation, Action action, StockDailyPriceResponse bar) {
        if (calculation.executed.contains(action.id()) || action.targetPrice() < bar.low()
                || action.targetPrice() > bar.high()) return;

        long amount = Math.multiplyExact(action.targetPrice(), (long) action.quantity());
        if (action.type() == ActionType.BUY) {
            executeBuy(calculation, action, bar.date(), amount);
        } else {
            executeSell(calculation, action, bar.date(), amount);
        }
    }

    private void executeBuy(Calculation calculation, Action action, LocalDate date, long amount) {
        if (calculation.cash < amount) {
            calculation.skipped.add(new SkippedAction(action.id(), date, SkipReason.INSUFFICIENT_CASH));
            return;
        }
        calculation.quantity = Math.addExact(calculation.quantity, action.quantity());
        calculation.cost = Math.addExact(calculation.cost, amount);
        calculation.average = calculation.cost / calculation.quantity;
        calculation.cash -= amount;
        recordExecution(calculation, action, date, amount, 0, 0);
    }

    private void executeSell(Calculation calculation, Action action, LocalDate date, long amount) {
        if (calculation.quantity < action.quantity()) {
            calculation.skipped.add(new SkippedAction(action.id(), date, SkipReason.INSUFFICIENT_QUANTITY));
            return;
        }
        // 전량 매도에서는 평균단가 절삭으로 남은 원가도 모두 반영한다.
        long soldCost = calculation.quantity == action.quantity() ? calculation.cost
                : Math.multiplyExact(calculation.average, (long) action.quantity());
        calculation.quantity -= action.quantity();
        calculation.cost -= soldCost;
        if (calculation.quantity == 0) calculation.average = 0;
        calculation.cash = Math.addExact(calculation.cash, amount);
        long profit = Math.subtractExact(amount, soldCost);
        calculation.realized = Math.addExact(calculation.realized, profit);
        recordExecution(calculation, action, date, amount, soldCost, profit);
    }

    private void recordExecution(Calculation calculation, Action action, LocalDate date,
                                 long amount, long soldCost, long profit) {
        calculation.executed.add(action.id());
        calculation.executions.add(new Execution(action.id(), date, action.type(), action.targetPrice(),
                action.quantity(), amount, soldCost, profit));
    }

    private Result buildResult(Calculation calculation, LocalDate processedThrough,
                               NavigableMap<LocalDate, StockDailyPriceResponse> bars) {
        // 데이터가 없는 날을 처리 완료로 추측하지 않는다. 누락/휴장일 관리는 수집 계층에서 담당한다.
        LocalDate processed = bars.isEmpty() ? processedThrough : bars.lastKey();
        var state = new State(calculation.cash, calculation.quantity, calculation.cost, calculation.average,
                calculation.realized, processed, calculation.executed);
        return new Result(state, calculation.executions, calculation.skipped);
    }

    // 호출마다 생성하는 작업 상태. 외부 입력과 계산기 인스턴스에는 변경 가능한 상태를 두지 않는다.
    private static class Calculation {
        private long cash;
        private int quantity;
        private long cost;
        private long average;
        private long realized;
        private final Set<Long> executed;
        private final List<Execution> executions = new ArrayList<>();
        private final List<SkippedAction> skipped = new ArrayList<>();

        private Calculation(State initial) {
            cash = initial.cash();
            quantity = initial.quantity();
            cost = initial.costBasis();
            average = initial.averagePrice();
            realized = initial.realizedProfit();
            executed = new HashSet<>(initial.executedActionIds());
        }
    }
}
