package com.project.investment_tracker.repository;

import com.project.investment_tracker.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    boolean existsByPlanActionId(Long planActionId);
    List<Trade> findByAccountIdOrderByTradeDateTimeAscIdAsc(Long accountId);
    @Query("select coalesce(sum(t.quantity), 0) from Trade t where t.planAction.id = :planActionId")
    long sumQuantityByPlanActionId(@Param("planActionId") Long planActionId);
    List<Trade> findByAccountIdAndStockSymbolOrderByTradeDateTimeAscIdAsc(Long accountId, String stockSymbol);
    boolean existsByAccountIdAndStockSymbolAndTradeDateTimeAfter(
            Long accountId,
            String stockSymbol,
            LocalDateTime tradeDateTime
    );
}
