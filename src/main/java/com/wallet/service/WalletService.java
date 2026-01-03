package com.wallet.service;

import com.wallet.dto.WalletBalanceResponse;
import com.wallet.dto.WalletOperationRequest;
import com.wallet.dto.WalletOperationResponse;
import com.wallet.entity.Wallet;
import com.wallet.exception.InsufficientFundsException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {
    
    private final WalletRepository walletRepository;
    
    @Transactional
    @Retryable(
        retryFor = {org.springframework.dao.OptimisticLockingFailureException.class},
        maxAttempts = 10,
        backoff = @Backoff(delay = 10, multiplier = 1.5)
    )
    public WalletOperationResponse processOperation(WalletOperationRequest request) {
        log.info("Processing {} operation for wallet: {}, amount: {}", 
            request.getOperationType(), request.getWalletId(), request.getAmount());
        
        Wallet wallet = walletRepository.findByIdWithLock(request.getWalletId())
            .orElseThrow(() -> new WalletNotFoundException(request.getWalletId()));
        
        BigDecimal newBalance;
        if (request.getOperationType() == WalletOperationRequest.OperationType.DEPOSIT) {
            newBalance = wallet.deposit(request.getAmount()); 
            log.info("Deposit completed. New balance: {}", newBalance);
        } else {
            newBalance = wallet.withdraw(request.getAmount()); 
            log.info("Withdrawal completed. New balance: {}", newBalance);
        }
        
        wallet = walletRepository.save(wallet);
        
        return WalletOperationResponse.of(
            wallet.getId(),
            wallet.getBalance(),
            request.getOperationType(),
            request.getAmount()
        );
    }
    
    @Transactional(readOnly = true)
    public WalletBalanceResponse getWalletBalance(UUID walletId) {
        log.debug("Getting balance for wallet: {}", walletId);
        
        Wallet wallet = walletRepository.findById(walletId)
            .orElseThrow(() -> new WalletNotFoundException(walletId));
        
        return WalletBalanceResponse.of(wallet.getId(), wallet.getBalance());
    }
}

