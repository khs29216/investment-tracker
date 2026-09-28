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

        applyCashEffect(account, TradeCommand.from(request));

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
        rebuildStockHolding(account, trade.getStockSymbol(), trade.getStockName());

        if (planAction != null) {
            planAction.execute();
        }

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

        String previousStockSymbol = trade.getStockSymbol();
        String previousStockName = trade.getStockName();
        applyCashEffect(trade.getAccount(), reverse(TradeCommand.from(trade)));
        applyCashEffect(trade.getAccount(), TradeCommand.from(request));

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

        rebuildStockHolding(trade.getAccount(), previousStockSymbol, previousStockName);
        if (!previousStockSymbol.equals(trade.getStockSymbol())) {
            rebuildStockHolding(trade.getAccount(), trade.getStockSymbol(), trade.getStockName());
        }

        return TradeResponse.from(trade);
    }

    @Transactional
    public void deleteTrade(Long id) {
        Trade trade = tradeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.TRADE_NOT_FOUND));

        validateNoLaterTrade(trade);
        applyCashEffect(trade.getAccount(), reverse(TradeCommand.from(trade)));

        tradeRepository.delete(trade);
        rebuildStockHolding(trade.getAccount(), trade.getStockSymbol(), trade.getStockName());
    }

    private void validateNoLaterTrade(Trade trade) {
        boolean existsLaterTrade = tradeRepository
                .findByAccountIdAndStockSymbolOrderByTradeDateTimeAscIdAsc(
                        trade.getAccount().getId(), trade.getStockSymbol())
                .stream()
                .anyMatch(other -> other.getTradeDateTime().isAfter(trade.getTradeDateTime())
                        || (other.getTradeDateTime().equals(trade.getTradeDateTime())
                        && other.getId() > trade.getId()));

        if (existsLaterTrade) {
            throw new BadRequestException(ErrorMessage.TRADE_HAS_LATER_TRADE);
        }
    }

    private void applyCashEffect(Account account, TradeCommand command) {
        int amount = calculateTradeAmount(command);
        if (command.tradeType() == TradeType.BUY) {
            account.decreaseCash(amount);
        } else {
            account.increaseCash(amount);
        }
    }

    private void rebuildStockHolding(Account account, String stockSymbol, String stockName) {
        // Replay remaining trades instead of treating cancellation as an opposite trade.
        List<Trade> trades = tradeRepository.findByAccountIdAndStockSymbolOrderByTradeDateTimeAscIdAsc(
                account.getId(), stockSymbol);
        StockHolding stockHolding = stockHoldingRepository
                .findByAccountIdAndStockSymbol(account.getId(), stockSymbol)
                .orElseGet(() -> new StockHolding(account, stockName, stockSymbol, 0, 0));
        stockHolding.resetPosition();
        for (Trade remainingTrade : trades) {
            if (remainingTrade.getTradeType() == TradeType.BUY) {
                stockHolding.buy(remainingTrade.getTradePrice(), remainingTrade.getQuantity());
            } else {
                stockHolding.sell(remainingTrade.getQuantity());
            }
        }
        stockHoldingRepository.save(stockHolding);
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
