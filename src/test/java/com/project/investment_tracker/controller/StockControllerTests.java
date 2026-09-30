package com.project.investment_tracker.controller;

import com.project.investment_tracker.dto.StockPriceResponse;
import com.project.investment_tracker.external.kis.KisStockMaster;
import com.project.investment_tracker.external.kis.KisStockMasterClient;
import com.project.investment_tracker.global.error.ErrorMessage;
import com.project.investment_tracker.global.error.GlobalExceptionHandler;
import com.project.investment_tracker.service.StockPriceService;
import com.project.investment_tracker.service.StockSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StockControllerTests {
    private final KisStockMasterClient client = mock(KisStockMasterClient.class);
    private final StockPriceService priceService = mock(StockPriceService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(
                    new StockController(priceService, new StockSearchService(client)))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test
    @DisplayName("검색 API는 종목명, 코드, 시장을 JSON 목록으로 반환한다")
    void returnsSearchResults() throws Exception {
        when(client.loadStockMasters()).thenReturn(List.of(
                new KisStockMaster("삼성전자", "005930", KisStockMaster.Market.KOSPI)));
        mvc.perform(get("/api/stocks/search").param("keyword", "삼성"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].stockName").value("삼성전자"))
                .andExpect(jsonPath("$[0].stockSymbol").value("005930"))
                .andExpect(jsonPath("$[0].market").value("KOSPI"));
    }

    @Test
    @DisplayName("검색어가 없으면 빈 JSON 목록을 반환한다")
    void missingKeywordReturnsEmptyList() throws Exception {
        mvc.perform(get("/api/stocks/search"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("종목 목록 다운로드 실패는 503과 공통 오류 응답을 반환한다")
    void downloadFailureReturnsServiceUnavailable() throws Exception {
        when(client.loadStockMasters()).thenThrow(new IllegalStateException("download failed"));
        mvc.perform(get("/api/stocks/search").param("keyword", "삼성"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(ErrorMessage.STOCK_SEARCH_UNAVAILABLE));
    }

    @Test
    @DisplayName("기존 현재가 조회 API는 그대로 동작한다")
    void priceEndpointStillWorks() throws Exception {
        when(priceService.getStockPrice("005930")).thenReturn(new StockPriceResponse("005930", 65000L));
        mvc.perform(get("/api/stocks/005930/price"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentPrice").value(65000));
        verifyNoInteractions(client);
    }
}
