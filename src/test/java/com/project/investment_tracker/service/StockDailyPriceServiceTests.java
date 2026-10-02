package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.StockDailyPriceResponse;
import com.project.investment_tracker.external.kis.KisDailyPriceClient;
import com.project.investment_tracker.global.error.BadRequestException;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StockDailyPriceServiceTests {
    @Test
    void splitsLongRangeSortsAndDeduplicates() {
        var client = mock(KisDailyPriceClient.class);
        var start = LocalDate.of(2025, 1, 1);
        var first = new StockDailyPriceResponse("005930", start, 100, 120, 90, 110);
        var second = new StockDailyPriceResponse("005930", start.plusDays(1), 100, 120, 90, 110);
        when(client.getDailyPrices("005930", start, start.plusDays(99)))
                .thenReturn(List.of(second, first, first));
        when(client.getDailyPrices("005930", start.plusDays(100), start.plusDays(150)))
                .thenReturn(List.of());
        assertEquals(List.of(first, second), new StockDailyPriceService(client)
                .getDailyPrices("005930", start, start.plusDays(150)));
        verify(client, times(2)).getDailyPrices(eq("005930"), any(), any());
    }

    @Test
    void rejectsInvalidRangesBeforeCallingKis() {
        var client = mock(KisDailyPriceClient.class);
        var service = new StockDailyPriceService(client);
        var start = LocalDate.of(2025, 1, 1);
        assertThrows(BadRequestException.class, () -> service.getDailyPrices("005930", start, start.minusDays(1)));
        assertThrows(BadRequestException.class, () -> service.getDailyPrices("005930", start, start.plusYears(2)));
        assertThrows(BadRequestException.class, () -> service.getDailyPrices("invalid", start, start));
        assertThrows(BadRequestException.class, () -> service.getDailyPrices("005930", start, LocalDate.now().plusDays(1)));
        verifyNoInteractions(client);
    }
}
