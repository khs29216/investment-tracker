package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.TradePerformanceResponse;
import com.project.investment_tracker.global.error.ErrorMessage;
import com.project.investment_tracker.global.error.ResourceNotFoundException;
import com.project.investment_tracker.repository.AccountRepository;
import com.project.investment_tracker.repository.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TradePerformanceService {
    private final AccountRepository accountRepository;
    private final TradeRepository tradeRepository;
    private final TradePerformanceCalculator calculator;

    public TradePerformanceService(AccountRepository accountRepository, TradeRepository tradeRepository,
                                   TradePerformanceCalculator calculator) {
        this.accountRepository = accountRepository;
        this.tradeRepository = tradeRepository;
        this.calculator = calculator;
    }

    @Transactional(readOnly = true)
    public TradePerformanceResponse getPerformance(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND);
        }
        return calculator.calculate(accountId,
                tradeRepository.findByAccountIdOrderByTradeDateTimeAscIdAsc(accountId));
    }
}
