package com.project.investment_tracker.external.kis;

import org.springframework.stereotype.Component;
import java.util.function.Supplier;

@Component
public class KisQuoteRateLimiter {
    private long nextAllowedAt;

    public synchronized <T> T execute(Supplier<T> request) {
        try {
            long remaining;
            while ((remaining = nextAllowedAt - System.nanoTime()) > 0) {
                java.util.concurrent.TimeUnit.NANOSECONDS.sleep(remaining);
            }
            return request.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("시세 조회 대기가 중단되었습니다.", exception);
        } finally {
            nextAllowedAt = System.nanoTime() + 1_000_000_000L;
        }
    }
}
