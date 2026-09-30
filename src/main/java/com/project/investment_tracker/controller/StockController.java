package com.project.investment_tracker.controller;

import com.project.investment_tracker.dto.StockPriceResponse;
import com.project.investment_tracker.dto.StockSearchResponse;
import com.project.investment_tracker.service.StockPriceService;
import com.project.investment_tracker.service.StockSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockController {
    private final StockPriceService stockPriceService;
    private final StockSearchService stockSearchService;

    public StockController(StockPriceService stockPriceService, StockSearchService stockSearchService) {
        this.stockPriceService = stockPriceService;
        this.stockSearchService = stockSearchService;
    }

    @GetMapping("/search")
    public List<StockSearchResponse> searchStocks(@RequestParam(defaultValue = "") String keyword) {
        return stockSearchService.search(keyword);
    }

    @GetMapping("/{stockSymbol}/price")
    public StockPriceResponse getStockPrice(@PathVariable String stockSymbol) {
        return stockPriceService.getStockPrice(stockSymbol);
    }
}
