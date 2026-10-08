package com.project.investment_tracker.repository;

import com.project.investment_tracker.entity.InvestmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentPlanRepository extends JpaRepository<InvestmentPlan, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from InvestmentPlan p where p.id = :id")
    java.util.Optional<InvestmentPlan> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    java.util.List<InvestmentPlan> findByAccountIdAndStockSymbol(Long accountId, String stockSymbol);
}
