package com.project.investment_tracker.repository;

import com.project.investment_tracker.entity.PlanAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlanActionRepository extends JpaRepository<PlanAction, Long> {
    List<PlanAction> findByInvestmentPlanId(Long investmentPlanId);
}
