package com.project.investment_tracker.repository;

import com.project.investment_tracker.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select a from Account a where a.id = :id")
    java.util.Optional<Account> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
}
