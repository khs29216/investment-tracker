package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.StockPriceResponse;
import com.project.investment_tracker.external.kis.KisStockPriceClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class StockPriceService {

    private static final long CACHE_SECONDS = 30;
    private static final long MIN_API_INTERVAL_MILLIS = 1000;

    private final KisStockPriceClient kisStockPriceClient;
    private final Map<String, CachedStockPrice> stockPriceCache = new HashMap<>();

    private LocalDateTime lastApiCalledAt;

    public StockPriceService(KisStockPriceClient kisStockPriceClient) {
        this.kisStockPriceClient = kisStockPriceClient;
    }

    public StockPriceResponse getStockPrice(String stockSymbol) {
        CachedStockPrice cachedStockPrice = stockPriceCache.get(stockSymbol);

        if (cachedStockPrice != null && !cachedStockPrice.isExpired()) {
            return new StockPriceResponse(
                    stockSymbol,
                    cachedStockPrice.currentPrice()
            );
        }

        waitForRateLimit();

        Integer currentPrice = kisStockPriceClient.getCurrentPrice(stockSymbol);
        lastApiCalledAt = LocalDateTime.now();

        stockPriceCache.put(
                stockSymbol,
                new CachedStockPrice(
                        currentPrice,
                        LocalDateTime.now().plusSeconds(CACHE_SECONDS)
                )
        );

        return new StockPriceResponse(
                stockSymbol,
                currentPrice
        );
    }

    private void waitForRateLimit() {
        if (lastApiCalledAt == null) {
            return;
        }

        long elapsedMillis = Duration.between(lastApiCalledAt, LocalDateTime.now()).toMillis();

        if (elapsedMillis >= MIN_API_INTERVAL_MILLIS) {
            return;
        }

        try {
            Thread.sleep(MIN_API_INTERVAL_MILLIS - elapsedMillis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("현재가 조회 대기 중 인터럽트가 발생했습니다.", exception);
        }
    }

    private record CachedStockPrice(
            Integer currentPrice,
            LocalDateTime expiresAt
    ) {
        private boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
}
