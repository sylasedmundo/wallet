package com.wallet.exception;

import java.math.BigDecimal;
import java.util.UUID;

public class InsufficientFundsException extends RuntimeException {
    
    public InsufficientFundsException(UUID walletId, BigDecimal balance, BigDecimal requestedAmount) {
        super(String.format("Insufficient funds in wallet %s. Balance: %s, Requested: %s", 
            walletId, balance, requestedAmount));
    }
}

