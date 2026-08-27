package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.AccountCreateRequest;
import com.project.investment_tracker.dto.AccountResponse;
import com.project.investment_tracker.dto.AccountSummaryResponse;
import com.project.investment_tracker.dto.AccountUpdateRequest;
import com.project.investment_tracker.dto.StockHoldingResponse;
import com.project.investment_tracker.entity.Account;
import com.project.investment_tracker.entity.StockHolding;
import com.project.investment_tracker.global.error.ErrorMessage;
import com.project.investment_tracker.global.error.ResourceNotFoundException;
import com.project.investment_tracker.repository.AccountRepository;
import com.project.investment_tracker.repository.StockHoldingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final StockHoldingRepository stockHoldingRepository;

    public AccountService(AccountRepository accountRepository, StockHoldingRepository stockHoldingRepository) {
        this.accountRepository = accountRepository;
        this.stockHoldingRepository = stockHoldingRepository;
    }

    public AccountResponse createAccount(AccountCreateRequest request) {
        Account account = new Account(
                request.accountName(),
                request.cashBalance()
        );

        Account savedAccount = accountRepository.save(account);

        return AccountResponse.from(savedAccount);
    }

    public AccountResponse getAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND));

        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse updateAccount(Long id, AccountUpdateRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND));

        account.update(request.accountName(), request.cashBalance());

        return AccountResponse.from(account);
    }

    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND));

        accountRepository.delete(account);
    }

    public AccountSummaryResponse getAccountSummary(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND));

        List<StockHolding> stockHoldings = stockHoldingRepository.findByAccountId(id);

        int totalInvestmentAmount = stockHoldings.stream()
                .mapToInt(StockHolding::getTotalInvestmentAmount)
                .sum();

        List<StockHoldingResponse> stockHoldingResponses = stockHoldings.stream()
                .map(StockHoldingResponse::from)
                .toList();

        return new AccountSummaryResponse(
                account.getId(),
                account.getAccountName(),
                account.getCashBalance(),
                totalInvestmentAmount,
                stockHoldingResponses
        );
    }
}
