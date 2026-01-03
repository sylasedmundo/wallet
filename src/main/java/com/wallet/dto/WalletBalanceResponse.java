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
public class WalletBalanceResponse {
    
    @JsonProperty("walletId")
    private UUID walletId;
    
    @JsonProperty("balance")
    private BigDecimal balance;
    
    public static WalletBalanceResponse of(UUID walletId, BigDecimal balance) {
        return WalletBalanceResponse.builder()
            .walletId(walletId)
            .balance(balance)
            .build();
    }
}