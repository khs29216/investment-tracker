package com.project.investment_tracker.service;


import com.project.investment_tracker.dto.InvestmentPlanCreateRequest;
import com.project.investment_tracker.dto.InvestmentPlanResponse;
import com.project.investment_tracker.dto.InvestmentPlanUpdateRequest;
import com.project.investment_tracker.entity.InvestmentPlan;
import com.project.investment_tracker.global.error.ErrorMessage;
import com.project.investment_tracker.global.error.ResourceNotFoundException;
import com.project.investment_tracker.repository.InvestmentPlanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;
import java.time.ZoneId;
import com.project.investment_tracker.global.error.BadRequestException;
import com.project.investment_tracker.repository.AccountRepository;
import com.project.investment_tracker.repository.StockHoldingRepository;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;

@Service
public class InvestmentPlanService {
    private final InvestmentPlanRepository investmentPlanRepository;
    private final AccountRepository accounts;
    private final StockHoldingRepository holdings;
    private final StockPriceService prices;
    private final TransactionTemplate transaction;

    public InvestmentPlanService(InvestmentPlanRepository investmentPlanRepository, AccountRepository accounts,
                                 StockHoldingRepository holdings, StockPriceService prices,
                                 PlatformTransactionManager manager) {
        this.investmentPlanRepository = investmentPlanRepository;
        this.accounts = accounts;
        this.holdings = holdings;
        this.prices = prices;
        this.transaction = new TransactionTemplate(manager);
    }

    public InvestmentPlanResponse createInvestmentPlan(InvestmentPlanCreateRequest request) {
        if (!accounts.existsById(request.accountId())) {
            throw new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND);
        }
        String symbol = request.stockSymbol().strip().toUpperCase(java.util.Locale.ROOT);
        if (!symbol.matches("[0-9A-Z]{6}")) throw new BadRequestException("종목 코드를 확인해주세요.");
        validateEndDate(request);
        // 외부 시세 응답을 기다리는 동안 DB 잠금을 잡지 않는다.
        long price = prices.getStockPrice(symbol).currentPrice();
        if (price <= 0) throw new BadRequestException("시작 가격을 확인할 수 없습니다.");
        return transaction.execute(status -> {
            var account = accounts.findByIdForUpdate(request.accountId())
                    .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND));
            var now = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
            validateEndDate(request);
            for (var existing : investmentPlanRepository.findByAccountIdAndStockSymbol(account.getId(), symbol)) {
                if (existing.getEndedAt() == null || !existing.getEndedAt().toLocalDate().isBefore(now.toLocalDate())) {
                    throw new BadRequestException("같은 종목의 기존 계획 종료일 다음 날부터 새 계획을 생성할 수 있습니다.");
                }
            }
            var holding = holdings.findByAccountIdAndStockSymbol(account.getId(), symbol).orElse(null);
            var plan = new InvestmentPlan(account, request.stockName(), symbol, request.totalBudget(), request.reason(),
                    holding == null ? 0 : holding.getQuantity(),
                    holding == null ? 0L : holding.getTotalInvestmentAmount(), price, now, request.plannedEndDate());
            return InvestmentPlanResponse.from(investmentPlanRepository.save(plan));
        });
    }

    private void validateEndDate(InvestmentPlanCreateRequest request) {
        if (request.plannedEndDate() == null || !request.plannedEndDate().isAfter(
                java.time.LocalDate.now(ZoneId.of("Asia/Seoul")))) {
            throw new BadRequestException("예정 종료일은 생성일 이후여야 합니다.");
        }
    }

    @Transactional
    public InvestmentPlanResponse closePlan(Long id) {
        var plan = investmentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVESTMENT_PLAN_NOT_FOUND));
        if (plan.getAccount() != null) accounts.findByIdForUpdate(plan.getAccount().getId());
        plan.close(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
        return InvestmentPlanResponse.from(plan);
    }

    @Transactional(readOnly = true)
    public List<InvestmentPlanResponse> getPlans() {
        return investmentPlanRepository.findAll()
                .stream()
                .map(InvestmentPlanResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public InvestmentPlanResponse getPlan(Long id) {
        InvestmentPlan investmentPlan = investmentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVESTMENT_PLAN_NOT_FOUND));

        return InvestmentPlanResponse.from(investmentPlan);
    }

    @Transactional
    public InvestmentPlanResponse updatePlan(Long id, InvestmentPlanUpdateRequest request) {
        InvestmentPlan investmentPlan = investmentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVESTMENT_PLAN_NOT_FOUND));

        investmentPlan.update(
                request.stockName(),
                request.stockSymbol(),
                request.totalBudget(),
                request.reason()

        );

        return InvestmentPlanResponse.from(investmentPlan);
    }

    public void deletePlan(Long id) {
        InvestmentPlan investmentPlan = investmentPlanRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.INVESTMENT_PLAN_NOT_FOUND));

        if (investmentPlan.getStartedAt() != null) {
            throw new BadRequestException("시작한 계획은 삭제하지 않고 종료하여 이력을 보존합니다.");
        }
        investmentPlanRepository.delete(investmentPlan);
    }
}
