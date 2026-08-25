package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.StockHoldingResponse;
import com.project.investment_tracker.global.error.ErrorMessage;
import com.project.investment_tracker.global.error.ResourceNotFoundException;
import com.project.investment_tracker.repository.AccountRepository;
import com.project.investment_tracker.repository.StockHoldingRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StockHoldingService {

    private final StockHoldingRepository stockHoldingRepository;
    private final AccountRepository accountRepository;

    public StockHoldingService(
            StockHoldingRepository stockHoldingRepository,
            AccountRepository accountRepository
    ) {
        this.stockHoldingRepository = stockHoldingRepository;
        this.accountRepository = accountRepository;
    }

    public List<StockHoldingResponse> getStockHoldings(Long accountId) {
        validateAccountExists(accountId);

        return stockHoldingRepository.findByAccountId(accountId)
                .stream()
                .map(StockHoldingResponse::from)
                .toList();
    }

    private void validateAccountExists(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND);
        }
    }
}
