package com.project.investment_tracker.service;

import com.project.investment_tracker.dto.AccountCreateRequest;
import com.project.investment_tracker.dto.AccountResponse;
import com.project.investment_tracker.dto.AccountUpdateRequest;
import com.project.investment_tracker.entity.Account;
import com.project.investment_tracker.global.error.ErrorMessage;
import com.project.investment_tracker.global.error.ResourceNotFoundException;
import com.project.investment_tracker.repository.AccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {

    private final AccountRepository accountRepository;

    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    public AccountResponse createAccount(AccountCreateRequest request) {
        Account account = new Account(
                request.accountName(),
                request.cashBalance()
        );

        Account savedAccount = accountRepository.save(account);

        return AccountResponse.from(savedAccount);
    }

    public AccountResponse getAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND));

        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse updateAccount(Long id, AccountUpdateRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND));

        account.update(request.accountName(), request.cashBalance());

        return AccountResponse.from(account);
    }

    public void deleteAccount(Long id) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessage.ACCOUNT_NOT_FOUND));

        accountRepository.delete(account);
    }
}
