package com.project.investment_tracker.repository;

import com.project.investment_tracker.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
}
