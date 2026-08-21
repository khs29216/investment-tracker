package com.project.investment_tracker.repository;

import com.project.investment_tracker.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    boolean existsByAccountIdAndStockSymbolAndTradeDateTimeAfter(
            Long accountId,
            String stockSymbol,
            LocalDateTime tradeDateTime
    );
}
