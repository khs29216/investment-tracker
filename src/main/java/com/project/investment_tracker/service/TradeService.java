package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.TradeCreateRequest;
import com.project.investment_tracker.dto.TradeResponse;
import com.project.investment_tracker.dto.TradeUpdateRequest;
import com.project.investment_tracker.entity.Account;
import com.project.investment_tracker.entity.PlanAction;
import com.project.investment_tracker.entity.StockHolding;
import com.project.investment_tracker.entity.Trade;
import com.project.investment_tracker.entity.TradeType;
import com.project.investment_tracker.global.error.BadRequestException;
import com.project.investment_tracker.global.error.ErrorMessage;
import com.project.investment_tracker.global.error.ResourceNotFoundException;
import com.project.investment_tracker.repository.AccountRepository;
import com.project.investment_tracker.repository.PlanActionRepository;
import com.project.investment_tracker.repository.StockHoldingRepository;
import com.project.investment_tracker.repository.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TradeService {
    private final TradeRepository tradeRepository;
    private final PlanActionRepository planActionRepository;
    private final AccountRepository accountRepository;
    private final StockHoldingRepository stockHoldingRepository;

    public TradeService(
            TradeRepository tradeRepository,
            PlanActionRepository planActionRepository,
            AccountRepository accountRepository,
            StockHoldingRepository stockHoldingRepository
    ) {
        this.tradeRepository = tradeRepository;
        this.planActionRepository = planActionRepository;
        this.accountRepository = accountRepository;
        this.stockHoldingRepository = stockHoldingRepository;
    }

    private Account findAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND));
    }

    private PlanAction findPlanActionOrNull(Long planActionId) {
        if (planActionId == null) {
            return null;
        }

        return planActionRepository.findById(planActionId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.PLAN_ACTION_NOT_FOUND));
    }

    @Transactional
    public TradeResponse createTrade(TradeCreateRequest request) {
        Account account = findAccount(request.accountId());
        PlanAction planAction = findPlanActionOrNull(request.planActionId());

        applyTradeToAccount(account, TradeCommand.from(request));

        Trade trade = new Trade(
                account,
                request.stockName(),
                request.stockSymbol(),
                request.tradeType(),
                request.tradePrice(),
                request.quantity(),
                request.tradeDateTime(),
                request.memo(),
                planAction
        );

        Trade savedTrade = tradeRepository.save(trade);

        return TradeResponse.from(savedTrade);
    }

    public List<TradeResponse> getTrades() {
        return tradeRepository.findAll()
                .stream()
                .map(TradeResponse::from)
                .toList();
    }

    public TradeResponse getTrade(Long id) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.TRADE_NOT_FOUND));

        return TradeResponse.from(trade);
    }

    @Transactional
    public TradeResponse updateTrade(Long id, TradeUpdateRequest request) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.TRADE_NOT_FOUND));

        validateNoLaterTrade(trade);

        rollbackTradeFromAccount(trade);
        applyTradeToAccount(trade.getAccount(), TradeCommand.from(request));

        PlanAction planAction = findPlanActionOrNull(request.planActionId());

        trade.update(
                request.stockName(),
                request.stockSymbol(),
                request.tradeType(),
                request.tradePrice(),
                request.quantity(),
                request.tradeDateTime(),
                request.memo(),
                planAction
        );

        return TradeResponse.from(trade);
    }

    @Transactional
    public void deleteTrade(Long id) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.TRADE_NOT_FOUND));

        validateNoLaterTrade(trade);
        rollbackTradeFromAccount(trade);

        tradeRepository.delete(trade);
    }

    private void validateNoLaterTrade(Trade trade) {
        boolean existsLaterTrade = tradeRepository.existsByAccountIdAndStockSymbolAndTradeDateTimeAfter(
                trade.getAccount().getId(),
                trade.getStockSymbol(),
                trade.getTradeDateTime()
        );

        if (existsLaterTrade) {
            throw new BadRequestException(ErrorMessage.TRADE_HAS_LATER_TRADE);
        }
    }

    private void rollbackTradeFromAccount(Trade trade) {
        TradeCommand command = TradeCommand.from(trade);
        TradeCommand rollbackCommand = reverse(command);

        applyTradeToAccount(trade.getAccount(), rollbackCommand);
    }

    private void applyTradeToAccount(Account account, TradeCommand command) {
        if (command.tradeType() == TradeType.BUY) {
            applyBuyEffect(account, command);
        }

        if (command.tradeType() == TradeType.SELL) {
            applySellEffect(account, command);
        }
    }

    private void applyBuyEffect(Account account, TradeCommand command) {
        int tradeAmount = calculateTradeAmount(command);

        account.decreaseCash(tradeAmount);

        StockHolding stockHolding = stockHoldingRepository
                .findByAccountIdAndStockSymbol(account.getId(), command.stockSymbol())
                .orElseGet(() -> new StockHolding(
                        account,
                        command.stockName(),
                        command.stockSymbol(),
                        0,
                        0
                ));

        stockHolding.buy(command.tradePrice(), command.quantity());
        stockHoldingRepository.save(stockHolding);
    }

    private void applySellEffect(Account account, TradeCommand command) {
        int tradeAmount = calculateTradeAmount(command);

        StockHolding stockHolding = stockHoldingRepository
                .findByAccountIdAndStockSymbol(account.getId(), command.stockSymbol())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.STOCK_HOLDING_NOT_FOUND));

        stockHolding.sell(command.quantity());
        account.increaseCash(tradeAmount);
    }

    private TradeCommand reverse(TradeCommand command) {
        TradeType reversedTradeType = command.tradeType() == TradeType.BUY
                ? TradeType.SELL
                : TradeType.BUY;

        return new TradeCommand(
                command.stockName(),
                command.stockSymbol(),
                reversedTradeType,
                command.tradePrice(),
                command.quantity()
        );
    }

    private int calculateTradeAmount(TradeCommand command) {
        return command.tradePrice() * command.quantity();
    }

    private record TradeCommand(
            String stockName,
            String stockSymbol,
            TradeType tradeType,
            Integer tradePrice,
            Integer quantity
    ) {
        private static TradeCommand from(TradeCreateRequest request) {
            return new TradeCommand(
                    request.stockName(),
                    request.stockSymbol(),
                    request.tradeType(),
                    request.tradePrice(),
                    request.quantity()
            );
        }

        private static TradeCommand from(TradeUpdateRequest request) {
            return new TradeCommand(
                    request.stockName(),
                    request.stockSymbol(),
                    request.tradeType(),
                    request.tradePrice(),
                    request.quantity()
            );
        }

        private static TradeCommand from(Trade trade) {
            return new TradeCommand(
                    trade.getStockName(),
                    trade.getStockSymbol(),
                    trade.getTradeType(),
                    trade.getTradePrice(),
                    trade.getQuantity()
            );
        }
    }
}
