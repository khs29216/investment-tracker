package com.project.investment_tracker.global.error;

public class StockPriceUnavailableException extends RuntimeException {
    public StockPriceUnavailableException(Throwable cause) {
        super("과거 시세를 불러올 수 없습니다. 잠시 후 다시 시도해주세요.", cause);
    }
}
