package com.project.investment_tracker.global.error;

public class StockSearchUnavailableException extends RuntimeException {
    public StockSearchUnavailableException(Throwable cause) {
        super(ErrorMessage.STOCK_SEARCH_UNAVAILABLE, cause);
    }
}
