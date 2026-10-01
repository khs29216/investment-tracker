package com.project.investment_tracker.controller;

import com.project.investment_tracker.dto.TradePerformanceResponse;
import com.project.investment_tracker.service.TradePerformanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account/{accountId}/performance")
public class TradePerformanceController {
    private final TradePerformanceService service;

    public TradePerformanceController(TradePerformanceService service) {
        this.service = service;
    }

    @GetMapping
    public TradePerformanceResponse getPerformance(@PathVariable Long accountId) {
        return service.getPerformance(accountId);
    }
}
