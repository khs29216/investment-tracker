package com.project.investment_tracker.repository;

import com.project.investment_tracker.entity.PlanActionSimulation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlanActionSimulationRepository extends JpaRepository<PlanActionSimulation, Long> {
    Optional<PlanActionSimulation> findByPlanActionId(Long actionId);
}
