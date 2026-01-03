package com.wallet.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
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
public class WalletOperationRequest {
    
    @NotNull(message = "walletId is required")
    @JsonProperty("walletId")
    private UUID walletId;
    
    @NotNull(message = "operationType is required")
    @JsonProperty("operationType")
    private OperationType operationType;
    
    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be greater than 0")
    @JsonProperty("amount")
    private BigDecimal amount;
    
    public static WalletOperationRequest of(UUID walletId, OperationType operationType, BigDecimal amount) {
        return WalletOperationRequest.builder()
            .walletId(walletId)
            .operationType(operationType)
            .amount(amount)
            .build();
    }
    
    public enum OperationType {
        DEPOSIT,
        WITHDRAW
    }
}
