package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.AccountDashboardResponse;
import com.project.investment_tracker.dto.StockHoldingDashboardResponse;
import com.project.investment_tracker.entity.Account;
import com.project.investment_tracker.entity.StockHolding;
import com.project.investment_tracker.global.error.ErrorMessage;
import com.project.investment_tracker.global.error.ResourceNotFoundException;
import com.project.investment_tracker.repository.AccountRepository;
import com.project.investment_tracker.repository.StockHoldingRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class AccountDashboardService {

    private final AccountRepository accountRepository;
    private final StockHoldingRepository stockHoldingRepository;
    private final StockPriceService stockPriceService;

    public AccountDashboardService(
            AccountRepository accountRepository,
            StockHoldingRepository stockHoldingRepository,
            StockPriceService stockPriceService
    ) {
        this.accountRepository = accountRepository;
        this.stockHoldingRepository = stockHoldingRepository;
        this.stockPriceService = stockPriceService;
    }

    public AccountDashboardResponse getAccountDashboard(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND));

        List<StockHoldingDashboardResponse> stockHoldings = stockHoldingRepository.findByAccountId(accountId)
                .stream()
                .map(this::toStockHoldingDashboardResponse)
                .toList();

        int totalInvestmentAmount = stockHoldings.stream()
                .mapToInt(StockHoldingDashboardResponse::investmentAmount)
                .sum();

        int totalEvaluationAmount = stockHoldings.stream()
                .mapToInt(StockHoldingDashboardResponse::evaluationAmount)
                .sum();

        int totalProfitLoss = totalEvaluationAmount - totalInvestmentAmount;

        return new AccountDashboardResponse(
                account.getId(),
                account.getAccountName(),
                account.getCashBalance(),
                totalInvestmentAmount,
                totalEvaluationAmount,
                totalProfitLoss,
                calculateReturnRate(totalProfitLoss, totalInvestmentAmount),
                stockHoldings
        );
    }

    private StockHoldingDashboardResponse toStockHoldingDashboardResponse(StockHolding stockHolding) {
        int currentPrice = stockPriceService.getStockPrice(stockHolding.getStockSymbol()).currentPrice();
        int investmentAmount = stockHolding.getTotalInvestmentAmount();
        int evaluationAmount = currentPrice * stockHolding.getQuantity();
        int profitLoss = evaluationAmount - investmentAmount;

        return new StockHoldingDashboardResponse(
                stockHolding.getStockName(),
                stockHolding.getStockSymbol(),
                stockHolding.getQuantity(),
                stockHolding.getAveragePrice(),
                currentPrice,
                investmentAmount,
                evaluationAmount,
                profitLoss,
                calculateReturnRate(profitLoss, investmentAmount)
        );
    }

    private BigDecimal calculateReturnRate(int profitLoss, int investmentAmount) {
        if (investmentAmount == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(profitLoss)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(investmentAmount), 2, RoundingMode.HALF_UP);
    }
}
