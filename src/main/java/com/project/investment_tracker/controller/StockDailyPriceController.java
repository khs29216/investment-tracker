package com.project.investment_tracker.controller;

import com.project.investment_tracker.dto.StockDailyPriceResponse;
import com.project.investment_tracker.service.StockDailyPriceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/stocks")
public class StockDailyPriceController {
    private final StockDailyPriceService service;
    public StockDailyPriceController(StockDailyPriceService service) { this.service = service; }

    @GetMapping("/{stockSymbol}/daily-prices")
    public List<StockDailyPriceResponse> getDailyPrices(@PathVariable String stockSymbol,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return service.getDailyPrices(stockSymbol, startDate, endDate);
    }
}
