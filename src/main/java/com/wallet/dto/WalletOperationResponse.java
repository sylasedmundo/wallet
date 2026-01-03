package com.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletOperationResponse {
    
    @JsonProperty("walletId")
    private UUID walletId;
    
    @JsonProperty("balance")
    private BigDecimal balance;
    
    @JsonProperty("operationType")
    private String operationType;
    
    @JsonProperty("amount")
    private BigDecimal amount;
    
    public static WalletOperationResponse of(UUID walletId, BigDecimal balance, String operationType, BigDecimal amount) {
        return WalletOperationResponse.builder()
            .walletId(walletId)
            .balance(balance)
            .operationType(operationType)
            .amount(amount)
            .build();
    }
}