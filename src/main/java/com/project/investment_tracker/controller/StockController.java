package com.project.investment_tracker.controller;

import com.project.investment_tracker.dto.StockPriceResponse;
import com.project.investment_tracker.service.StockPriceService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private final StockPriceService stockPriceService;

    public StockController(StockPriceService stockPriceService) {
        this.stockPriceService = stockPriceService;
    }

    @GetMapping("/{stockSymbol}/price")
    public StockPriceResponse getStockPrice(@PathVariable String stockSymbol) {
        return stockPriceService.getStockPrice(stockSymbol);
    }
}
