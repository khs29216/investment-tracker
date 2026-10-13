package com.project.investment_tracker.controller;

import com.project.investment_tracker.entity.ActionType;
import com.project.investment_tracker.global.error.*;
import com.project.investment_tracker.service.PlanSimulationService;
import com.project.investment_tracker.service.VirtualExecutionCalculator.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PlanSimulationControllerTests {
    private final PlanSimulationService service = mock(PlanSimulationService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new PlanSimulationController(service))
            .setControllerAdvice(new GlobalExceptionHandler()).build();
    private final LocalDate date = LocalDate.of(2026, 10, 12);

    @Test @DisplayName("실행 API는 누적 자산과 이번 실행 내역을 명시적인 응답 DTO로 반환한다")
    void returnsSimulation() throws Exception {
        when(service.simulate(1L)).thenReturn(new Result(new State(3000000000L, 0, 0, 0, 100L, date, Set.of(2L)),
                List.of(new Execution(2, date, ActionType.SELL, 200, 1, 200, 100, 100)),
                List.of(new SkippedAction(3, date, SkipReason.INSUFFICIENT_CASH))));
        mvc.perform(post("/api/plans/1/simulation"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.planId").value(1))
                .andExpect(jsonPath("$.cash").value(3000000000L))
                .andExpect(jsonPath("$.realizedProfit").value(100))
                .andExpect(jsonPath("$.processedThrough").value("2026-10-12"))
                .andExpect(jsonPath("$.executions[0].type").value("SELL"))
                .andExpect(jsonPath("$.executions[0].costBasis").value(100))
                .andExpect(jsonPath("$.skippedActions[0].reason").value("INSUFFICIENT_CASH"))
                .andExpect(jsonPath("$.executedActionIds").doesNotExist());
        verify(service).simulate(1L);
    }

    @Test @DisplayName("새 체결이 없는 실행도 누적 상태와 빈 목록을 반환한다")
    void returnsEmptyRun() throws Exception {
        when(service.simulate(1L)).thenReturn(new Result(State.initial(1000, 0, 0, date), List.of(), List.of()));
        mvc.perform(post("/api/plans/1/simulation"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.cash").value(1000))
                .andExpect(jsonPath("$.executions").isEmpty()).andExpect(jsonPath("$.skippedActions").isEmpty());
    }

    @Test @DisplayName("시작 전 계획과 없는 계획은 공통 400 및 404 오류를 반환한다")
    void returnsDomainErrors() throws Exception {
        when(service.simulate(1L)).thenThrow(new BadRequestException("시작한 계획만 시뮬레이션할 수 있습니다."));
        when(service.simulate(2L)).thenThrow(new ResourceNotFoundException(ErrorMessage.INVESTMENT_PLAN_NOT_FOUND));
        mvc.perform(post("/api/plans/1/simulation")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("시작한 계획만 시뮬레이션할 수 있습니다."));
        mvc.perform(post("/api/plans/2/simulation")).andExpect(status().isNotFound());
    }

    @Test @DisplayName("시세 실패는 503으로 응답하고 GET으로는 시뮬레이션을 실행하지 않는다")
    void returnsPriceFailureAndRejectsGet() throws Exception {
        when(service.simulate(1L)).thenThrow(new StockPriceUnavailableException(new IllegalStateException("test")));
        mvc.perform(post("/api/plans/1/simulation")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").exists());
        mvc.perform(get("/api/plans/1/simulation")).andExpect(status().isMethodNotAllowed());
        verify(service, times(1)).simulate(1L);
    }
}
