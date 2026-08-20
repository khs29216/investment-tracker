package com.project.investment_tracker.repository;

import com.project.investment_tracker.entity.CashTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashTransactionRepository extends JpaRepository<CashTransaction, Long> {
}
