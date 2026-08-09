package com.project.investment_tracker.repository;

import com.project.investment_tracker.entity.InvestmentPlan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvestmentPlanRepository extends JpaRepository<InvestmentPlan, Long> {
}
