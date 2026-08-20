package com.project.investment_tracker.repository;

import com.project.investment_tracker.entity.StockHolding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StockHoldingRepository extends JpaRepository<StockHolding, Long> {
    Optional<StockHolding> findByAccountIdAndStockSymbol(Long accountId, String stockSymbol);
}
