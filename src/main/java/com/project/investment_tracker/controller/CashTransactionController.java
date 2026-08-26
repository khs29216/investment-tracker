package com.project.investment_tracker.controller;

import com.project.investment_tracker.dto.CashTransactionCreateRequest;
import com.project.investment_tracker.dto.CashTransactionResponse;
import com.project.investment_tracker.service.CashTransactionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class CashTransactionController {

    private final CashTransactionService cashTransactionService;

    public CashTransactionController(CashTransactionService cashTransactionService) {
        this.cashTransactionService = cashTransactionService;
    }

    @PostMapping("/api/cash-transactions")
    public CashTransactionResponse createCashTransaction(@Valid @RequestBody CashTransactionCreateRequest request) {
        return cashTransactionService.createCashTransaction(request);
    }

    @GetMapping("/api/accounts/{accountId}/cash-transactions")
    public List<CashTransactionResponse> getCashTransactions(@PathVariable Long accountId) {
        return cashTransactionService.getCashTransactions(accountId);
    }
}
