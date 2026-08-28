package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.StockPriceResponse;
import com.project.investment_tracker.external.kis.KisStockPriceClient;
import org.springframework.stereotype.Service;

@Service
public class StockPriceService {

    private final KisStockPriceClient kisStockPriceClient;

    public StockPriceService(KisStockPriceClient kisStockPriceClient) {
        this.kisStockPriceClient = kisStockPriceClient;
    }

    public StockPriceResponse getStockPrice(String stockSymbol) {
        Integer currentPrice = kisStockPriceClient.getCurrentPrice(stockSymbol);

        return new StockPriceResponse(
                stockSymbol,
                currentPrice
        );
    }
}
