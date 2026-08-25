package com.project.investment_tracker.controller;

import com.project.investment_tracker.dto.StockHoldingResponse;
import com.project.investment_tracker.service.StockHoldingService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/{accountId}/stock-holdings")
public class StockHoldingController {

    private final StockHoldingService stockHoldingService;

    public StockHoldingController(StockHoldingService stockHoldingService) {
        this.stockHoldingService = stockHoldingService;
    }

    @GetMapping
    public List<StockHoldingResponse> getStockHoldings(@PathVariable Long accountId) {
        return stockHoldingService.getStockHoldings(accountId);
    }
}
