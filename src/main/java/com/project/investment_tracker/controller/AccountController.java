package com.project.investment_tracker.controller;

import com.project.investment_tracker.dto.AccountCreateRequest;
import com.project.investment_tracker.dto.AccountDashboardResponse;
import com.project.investment_tracker.dto.AccountResponse;
import com.project.investment_tracker.dto.AccountSummaryResponse;
import com.project.investment_tracker.dto.AccountUpdateRequest;
import com.project.investment_tracker.service.AccountDashboardService;
import com.project.investment_tracker.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountService accountService;
    private final AccountDashboardService accountDashboardService;

    public AccountController(
            AccountService accountService,
            AccountDashboardService accountDashboardService
    ) {
        this.accountService = accountService;
        this.accountDashboardService = accountDashboardService;
    }

    @PostMapping
    public AccountResponse createAccount(@Valid @RequestBody AccountCreateRequest request) {
        return accountService.createAccount(request);
    }

    @GetMapping("/{id}")
    public AccountResponse getAccount(@PathVariable Long id) {
        return accountService.getAccount(id);
    }

    @PutMapping("/{id}")
    public AccountResponse updateAccount(@PathVariable Long id, @Valid @RequestBody AccountUpdateRequest request) {
        return accountService.updateAccount(id, request);
    }

    @DeleteMapping("/{id}")
    public void deleteAccount(@PathVariable Long id) {
        accountService.deleteAccount(id);
    }

    @GetMapping("/{id}/summary")
    public AccountSummaryResponse getAccountSummary(@PathVariable Long id) {
        return accountService.getAccountSummary(id);
    }

    @GetMapping("/{id}/dashboard")
    public AccountDashboardResponse getAccountDashboard(@PathVariable Long id) {
        return accountDashboardService.getAccountDashboard(id);
    }

}
