package com.project.investment_tracker.repository;

import com.project.investment_tracker.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {
}
