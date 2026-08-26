package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.CashTransactionCreateRequest;
import com.project.investment_tracker.dto.CashTransactionResponse;
import com.project.investment_tracker.entity.Account;
import com.project.investment_tracker.entity.CashTransaction;
import com.project.investment_tracker.entity.CashTransactionType;
import com.project.investment_tracker.global.error.ErrorMessage;
import com.project.investment_tracker.global.error.ResourceNotFoundException;
import com.project.investment_tracker.repository.AccountRepository;
import com.project.investment_tracker.repository.CashTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CashTransactionService {

    private final CashTransactionRepository cashTransactionRepository;
    private final AccountRepository accountRepository;

    public CashTransactionService(
            CashTransactionRepository cashTransactionRepository,
            AccountRepository accountRepository
    ) {
        this.cashTransactionRepository = cashTransactionRepository;
        this.accountRepository = accountRepository;
    }

    @Transactional
    public CashTransactionResponse createCashTransaction(CashTransactionCreateRequest request) {
        Account account = findAccount(request.accountId());

        applyCashTransaction(account, request.type(), request.amount());

        CashTransaction cashTransaction = new CashTransaction(
                account,
                request.type(),
                request.amount(),
                LocalDateTime.now(),
                request.memo()
        );

        CashTransaction savedCashTransaction = cashTransactionRepository.save(cashTransaction);

        return CashTransactionResponse.from(savedCashTransaction);
    }

    public List<CashTransactionResponse> getCashTransactions(Long accountId) {
        validateAccountExists(accountId);

        return cashTransactionRepository.findByAccountId(accountId)
                .stream()
                .map(CashTransactionResponse::from)
                .toList();
    }

    private Account findAccount(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND));
    }

    private void validateAccountExists(Long accountId) {
        if (!accountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND);
        }
    }

    private void applyCashTransaction(Account account, CashTransactionType type, Integer amount) {
        if (type == CashTransactionType.DEPOSIT) {
            account.increaseCash(amount);
        }

        if (type == CashTransactionType.WITHDRAWAL) {
            account.decreaseCash(amount);
        }
    }
}
