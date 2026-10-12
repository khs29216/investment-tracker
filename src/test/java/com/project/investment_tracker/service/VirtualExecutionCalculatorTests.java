package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.StockDailyPriceResponse;
import com.project.investment_tracker.entity.ActionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static com.project.investment_tracker.service.VirtualExecutionCalculator.*;

class VirtualExecutionCalculatorTests {
    private final VirtualExecutionCalculator calculator = new VirtualExecutionCalculator();
    private final LocalDate start = LocalDate.of(2026, 1, 1);
    private Action buy(long id, long price, int qty) { return new Action(id, ActionType.BUY, price, qty); }
    private Action sell(long id, long price, int qty) { return new Action(id, ActionType.SELL, price, qty); }
    private StockDailyPriceResponse bar(int day, long low, long high) {
        return new StockDailyPriceResponse("005930", start.plusDays(day), low, high, low, high);
    }
    private Result run(State state, List<Action> actions, StockDailyPriceResponse... bars) {
        return calculator.calculate("005930", state, actions, List.of(bars), start.plusDays(10));
    }

    @Test @DisplayName("초기 10주에서 5주 매수 후 15주 매도하면 125만원 이익이다")
    void calculatesUserScenario() {
        var initial = State.initial(1000000, 10, 1000000, start);
        var actions = List.of(buy(1, 150000, 5), sell(2, 200000, 15));
        var first = run(initial, actions, bar(1, 140000, 160000));
        assertEquals(15, first.state().quantity());
        assertEquals(116666, first.state().averagePrice());
        assertEquals(1750000, first.state().costBasis());
        assertEquals(250000, first.state().cash());
        var result = run(first.state(), actions, bar(2, 190000, 210000));
        assertEquals(3250000, result.state().cash());
        assertEquals(1250000, result.state().realizedProfit());
        assertEquals(0, result.state().quantity());
        assertEquals(0, result.state().costBasis());
        assertEquals(1750000, result.executions().get(0).costBasis());
        assertEquals(10, initial.quantity());
    }

    @Test @DisplayName("같은 날은 ID 순서로 처리하여 앞선 매수 주식과 매도 대금을 바로 사용한다")
    void usesIdOrderAndSameDayResources() {
        var result = run(State.initial(100, 0, 0, start),
                List.of(buy(3, 100, 1), sell(2, 120, 1), buy(1, 100, 1)), bar(1, 100, 120));
        assertEquals(List.of(1L, 2L, 3L), result.executions().stream().map(Execution::actionId).toList());
        assertEquals(20, result.state().cash());
        assertEquals(1, result.state().quantity());
        assertEquals(20, result.state().realizedProfit());
    }

    @Test @DisplayName("수량 부족 매도는 부분 실행하지 않고 다음 접촉일에 재시도한다")
    void retriesOnLaterDayNotSameDay() {
        var actions = List.of(sell(1, 280000, 15), buy(2, 260000, 10));
        var first = run(State.initial(2600000, 5, 500000, start), actions, bar(1, 260000, 280000));
        assertEquals(List.of(2L), first.executions().stream().map(Execution::actionId).toList());
        assertEquals(SkipReason.INSUFFICIENT_QUANTITY, first.skippedActions().get(0).reason());
        assertEquals(15, first.state().quantity());
        var second = run(first.state(), actions, bar(2, 270000, 290000));
        assertEquals(1L, second.executions().get(0).actionId());
        assertEquals(0, second.state().quantity());
    }

    @Test @DisplayName("현금 부족 시 전량 미체결이며 뒤의 매도 후 다음 날 재시도한다")
    void retriesInsufficientCash() {
        var actions = List.of(buy(1, 100, 2), sell(2, 100, 1));
        var first = run(State.initial(100, 1, 100, start), actions, bar(1, 100, 100));
        assertEquals(SkipReason.INSUFFICIENT_CASH, first.skippedActions().get(0).reason());
        assertEquals(200, first.state().cash());
        var second = run(first.state(), actions, bar(2, 100, 100));
        assertEquals(2, second.state().quantity());
        assertEquals(0, second.state().cash());
    }

    @Test @DisplayName("시작 당일과 종료일 이후는 제외하고 이미 실행한 액션은 다시 실행하지 않는다")
    void respectsDateBoundsAndExecutedActions() {
        var actions = List.of(buy(1, 100, 1));
        var result = calculator.calculate("005930", State.initial(1000, 0, 0, start), actions,
                List.of(bar(0, 100, 100), bar(2, 100, 100), bar(1, 100, 100)), start.plusDays(1));
        assertEquals(1, result.executions().size());
        assertEquals(start.plusDays(1), result.executions().get(0).date());
        var retry = run(result.state(), actions, bar(1, 100, 100), bar(2, 100, 100));
        assertTrue(retry.executions().isEmpty());
        assertEquals(1, retry.state().quantity());
    }

    @Test @DisplayName("목표가가 범위 밖이면 미체결이고 중복 일봉은 한 번만 처리한다")
    void handlesGapsAndDuplicateBars() {
        var initial = State.initial(1000, 0, 0, start);
        assertTrue(run(initial, List.of(buy(1, 100, 1)), bar(1, 110, 120)).executions().isEmpty());
        assertEquals(1, run(initial, List.of(buy(1, 100, 1)), bar(1, 100, 120), bar(1, 100, 120)).executions().size());
        assertEquals(initial, run(initial, List.of()).state());
    }

    @Test @DisplayName("분할 계산과 한 번의 계산은 같은 최종 상태이며 매도 절삭 잔액을 보존한다")
    void splitEqualsFullReplay() {
        var initial = State.initial(1000, 2, 201, start);
        var actions = List.of(sell(1, 110, 1), sell(2, 120, 1));
        var full = run(initial, actions, bar(2, 120, 120), bar(1, 110, 110));
        var part = run(initial, actions, bar(1, 110, 110));
        var resumed = run(part.state(), actions, bar(2, 120, 120));
        assertEquals(full.state(), resumed.state());
        assertEquals(29, full.state().realizedProfit());
        assertEquals(101, resumed.executions().get(0).costBasis());
        assertEquals(Set.of(1L, 2L), full.state().executedActionIds());
    }

    @Test @DisplayName("잘못된 입력과 금액 오버플로는 결과를 반환하지 않고 실패한다")
    void rejectsInvalidInputsAndOverflow() {
        var initial = State.initial(100, 0, 0, start);
        assertThrows(IllegalArgumentException.class, () -> run(initial, List.of(buy(1, 100, 1), buy(1, 100, 1))));
        assertThrows(IllegalArgumentException.class, () -> run(initial, List.of(), bar(1, 120, 100)));
        assertThrows(ArithmeticException.class, () -> run(initial, List.of(buy(1, Long.MAX_VALUE, 2)), bar(1, 1, Long.MAX_VALUE)));
    }
}
