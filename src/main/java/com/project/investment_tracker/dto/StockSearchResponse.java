package com.project.investment_tracker.dto;

import com.project.investment_tracker.external.kis.KisStockMaster;

public record StockSearchResponse(String stockName, String stockSymbol, String market) {
    public static StockSearchResponse from(KisStockMaster stock) {
        return new StockSearchResponse(stock.stockName(), stock.stockSymbol(), stock.market().name());
    }
}
