package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.TradeCreateRequest;
import com.project.investment_tracker.dto.TradeResponse;
import com.project.investment_tracker.dto.TradeUpdateRequest;
import com.project.investment_tracker.entity.PlanAction;
import com.project.investment_tracker.entity.Trade;
import com.project.investment_tracker.repository.PlanActionRepository;
import com.project.investment_tracker.repository.TradeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TradeService {
    private final TradeRepository tradeRepository;
    private final PlanActionRepository planActionRepository;

    public TradeService(TradeRepository tradeRepository, PlanActionRepository planActionRepository) {
        this.tradeRepository = tradeRepository;
        this.planActionRepository = planActionRepository;
    }

    private PlanAction findPlanActionOrNull(Long planActionId) {
        if (planActionId == null) {
            return null;
        }

        return planActionRepository.findById(planActionId)
                .orElseThrow();
    }

    public TradeResponse createTrade(TradeCreateRequest request) {
        PlanAction planAction = findPlanActionOrNull(request.planActionId());

        Trade trade = new Trade(
                request.stockName(),
                request.stockSymbol(),
                request.tradeType(),
                request.tradePrice(),
                request.quantity(),
                request.tradeDate(),
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

    public TradeResponse getTrade(Long tradeId) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow();

        return TradeResponse.from(trade);
    }

    @Transactional
    public TradeResponse updateTrade(Long tradeId, TradeUpdateRequest request) {
        Trade trade = tradeRepository.findById(tradeId)
                .orElseThrow();

        PlanAction planAction = findPlanActionOrNull(request.planActionId());

        trade.update(
                request.stockName(),
                request.stockSymbol(),
                request.tradeType(),
                request.tradePrice(),
                request.quantity(),
                request.tradeDate(),
                request.memo(),
                planAction
        );

        return TradeResponse.from(trade);
    }

    public void deleteTrade(Long id) {
        tradeRepository.deleteById(id);
    }
}
