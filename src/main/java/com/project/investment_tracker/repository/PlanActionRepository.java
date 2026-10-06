package com.project.investment_tracker.repository;

import com.project.investment_tracker.entity.PlanAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanActionRepository extends JpaRepository<PlanAction, Long> {
    @org.springframework.data.jpa.repository.Query("select a.id from PlanAction a where a.simulation is null")
    List<Long> findIdsWithoutSimulation();
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from PlanAction a where a.id = :id")
    java.util.Optional<PlanAction> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    List<PlanAction> findByInvestmentPlanId(Long investmentPlanId);
}
