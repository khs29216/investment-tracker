package com.project.investment_tracker.service;

import com.project.investment_tracker.external.kis.KisStockMaster;
import com.project.investment_tracker.external.kis.KisStockMasterClient;
import com.project.investment_tracker.global.error.StockSearchUnavailableException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockSearchServiceTests {
    private final KisStockMasterClient client = mock(KisStockMasterClient.class);
    private final StockSearchService service = new StockSearchService(client);
    private final KisStockMaster samsung = new KisStockMaster("삼성전자", "005930", KisStockMaster.Market.KOSPI);

    @Test
    @DisplayName("종목명과 코드 일부로 검색하고 목록을 재사용한다")
    void searchesNameAndCodeUsingCachedList() {
        when(client.loadStockMasters()).thenReturn(List.of(samsung,
                new KisStockMaster("SK하이닉스", "000660", KisStockMaster.Market.KOSPI),
                new KisStockMaster("테스트", "123456", KisStockMaster.Market.KOSDAQ)));

        assertEquals("005930", service.search(" 삼성 ").get(0).stockSymbol());
        assertEquals("삼성전자", service.search("593").get(0).stockName());
        assertEquals("000660", service.search("sk").get(0).stockSymbol());
        assertEquals("KOSDAQ", service.search("테스트").get(0).market());
        assertTrue(service.search("없는종목").isEmpty());
        verify(client, times(1)).loadStockMasters();
    }

    @Test
    @DisplayName("빈 검색어는 다운로드 없이 빈 목록을 반환한다")
    void blankSearchDoesNotDownload() {
        assertTrue(service.search(null).isEmpty());
        assertTrue(service.search("").isEmpty());
        assertTrue(service.search("  ").isEmpty());
        verifyNoInteractions(client);
    }

    @Test
    @DisplayName("정확히 일치하는 종목을 먼저 반환하고 결과는 20개로 제한한다")
    void prioritizesExactMatchAndLimitsResults() {
        var stocks = IntStream.range(0, 30)
                .mapToObj(i -> new KisStockMaster("삼성전자" + i, String.format("%06d", i), KisStockMaster.Market.KOSPI))
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        stocks.add(samsung);
        when(client.loadStockMasters()).thenReturn(stocks);

        var results = service.search("삼성전자");
        assertEquals(20, results.size());
        assertEquals("005930", results.get(0).stockSymbol());
    }

    @Test
    @DisplayName("다운로드 실패는 캐시하지 않고 다음 검색에서 재시도한다")
    void retriesAfterFailure() {
        when(client.loadStockMasters()).thenThrow(new IllegalStateException("download failed"))
                .thenReturn(List.of(samsung));

        assertThrows(StockSearchUnavailableException.class, () -> service.search("삼성"));
        assertEquals(1, service.search("삼성").size());
        verify(client, times(2)).loadStockMasters();
    }

    @Test
    @DisplayName("동시에 첫 검색을 요청해도 종목 목록은 한 번만 불러온다")
    void concurrentSearchLoadsOnce() throws Exception {
        when(client.loadStockMasters()).thenReturn(List.of(samsung));
        var executor = Executors.newFixedThreadPool(4);
        var start = new CountDownLatch(1);
        try {
            var futures = IntStream.range(0, 4).mapToObj(i -> executor.submit(() -> {
                start.await();
                return service.search("삼성");
            })).toList();
            start.countDown();
            for (var future : futures) {
                assertEquals(1, future.get(5, TimeUnit.SECONDS).size());
            }
            verify(client, times(1)).loadStockMasters();
        } finally {
            executor.shutdownNow();
        }
    }
}
