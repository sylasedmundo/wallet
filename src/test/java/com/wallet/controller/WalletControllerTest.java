package com.wallet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.dto.WalletOperationRequest;
import com.wallet.entity.Wallet;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class WalletControllerTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");
    
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
    
    @Autowired
    private MockMvc mockMvc;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private WalletRepository walletRepository;
    
    private UUID walletId;
    private final BigDecimal initialBalance = new BigDecimal("1000.00");
    
    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
            .id(walletId)
            .balance(initialBalance)
            .version(0L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        walletRepository.save(wallet);
    }
    
    @Test
    void testDepositOperation() throws Exception {
        WalletOperationRequest request = new WalletOperationRequest(
            walletId,
            WalletOperationRequest.OperationType.DEPOSIT,
            new BigDecimal("500.00")
        );
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(1500.00))
                .andExpect(jsonPath("$.operationType").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(500.00));
    }
    
    @Test
    void testWithdrawOperation() throws Exception {
        WalletOperationRequest request = new WalletOperationRequest(
            walletId,
            WalletOperationRequest.OperationType.WITHDRAW,
            new BigDecimal("300.00")
        );
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(700.00))
                .andExpect(jsonPath("$.operationType").value("WITHDRAW"))
                .andExpect(jsonPath("$.amount").value(300.00));
    }
    
    @Test
    void testWithdrawInsufficientFunds() throws Exception {
        WalletOperationRequest request = new WalletOperationRequest(
            walletId,
            WalletOperationRequest.OperationType.WITHDRAW,
            new BigDecimal("2000.00")
        );
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Insufficient funds")));
    }
    
    @Test
    void testWalletNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            nonExistentId,
            WalletOperationRequest.OperationType.DEPOSIT,
            new BigDecimal("100.00")
        );
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("not found")));
    }
    
    @Test
    void testGetWalletBalance() throws Exception {
        mockMvc.perform(get("/api/v1/wallets/" + walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(1000.00));
    }
    
    @Test
    void testGetWalletBalanceNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        
        mockMvc.perform(get("/api/v1/wallets/" + nonExistentId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }
    
    @Test
    void testInvalidJson() throws Exception {
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ invalid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
    
    @Test
    void testMissingRequiredFields() throws Exception {
        String invalidRequest = "{\"walletId\":\"" + walletId + "\"}";
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.errors").isArray());
    }
    
    @Test
    void testInvalidAmount() throws Exception {
        WalletOperationRequest request = new WalletOperationRequest(
            walletId,
            WalletOperationRequest.OperationType.DEPOSIT,
            new BigDecimal("-100.00")
        );
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
    
    @Test
    void testInvalidOperationType() throws Exception {
        String invalidRequest = String.format(
            "{\"walletId\":\"%s\",\"operationType\":\"INVALID\",\"amount\":100.00}",
            walletId
        );
        
        mockMvc.perform(post("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
