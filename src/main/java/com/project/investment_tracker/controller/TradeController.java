package com.project.investment_tracker.controller;

import com.project.investment_tracker.dto.TradeCreateRequest;
import com.project.investment_tracker.dto.TradeResponse;
import com.project.investment_tracker.dto.TradeUpdateRequest;
import com.project.investment_tracker.service.TradeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;

    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping
    public TradeResponse createTrade(@Valid @RequestBody TradeCreateRequest request) {
        return tradeService.createTrade(request);
    }

    @GetMapping
    public List<TradeResponse> getTrades() {
        return tradeService.getTrades();
    }

    @GetMapping("/{tradeId}")
    public TradeResponse getTrade(@PathVariable Long tradeId) {
        return tradeService.getTrade(tradeId);
    }

    @PutMapping("/{tradeId}")
    public TradeResponse updateTrade(@PathVariable Long tradeId, @Valid @RequestBody TradeUpdateRequest request) {
        return tradeService.updateTrade(tradeId, request);
    }

    @DeleteMapping("/{tradeId}")
    public void deleteTrade(@PathVariable Long tradeId) {
        tradeService.deleteTrade(tradeId);
    }

}
