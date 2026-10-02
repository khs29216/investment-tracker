package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.StockPriceResponse;
import com.project.investment_tracker.external.kis.KisStockPriceClient;
import com.project.investment_tracker.external.kis.KisQuoteRateLimiter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class StockPriceService {

    private static final long CACHE_SECONDS = 30;

    private final KisStockPriceClient kisStockPriceClient;
    private final Map<String, CachedStockPrice> stockPriceCache = new HashMap<>();

    private final KisQuoteRateLimiter rateLimiter;

    public StockPriceService(KisStockPriceClient kisStockPriceClient, KisQuoteRateLimiter rateLimiter) {
        this.kisStockPriceClient = kisStockPriceClient;
        this.rateLimiter = rateLimiter;
    }

    public synchronized StockPriceResponse getStockPrice(String stockSymbol) {
        CachedStockPrice cachedStockPrice = stockPriceCache.get(stockSymbol);

        if (cachedStockPrice != null && !cachedStockPrice.isExpired()) {
            return new StockPriceResponse(
                    stockSymbol,
                    cachedStockPrice.currentPrice()
            );
        }

        Long currentPrice = rateLimiter.execute(() -> kisStockPriceClient.getCurrentPrice(stockSymbol));

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

    private record CachedStockPrice(
            Long currentPrice,
            LocalDateTime expiresAt
    ) {
        private boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
}
