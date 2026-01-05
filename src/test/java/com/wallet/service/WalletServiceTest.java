package com.wallet.service;

import com.wallet.dto.WalletOperationRequest;
import com.wallet.entity.Wallet;
import com.wallet.exception.InsufficientFundsException;
import com.wallet.exception.WalletNotFoundException;
import com.wallet.repository.WalletRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
class WalletServiceTest {
    
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
    private WalletService walletService;
    
    @Autowired
    private WalletRepository walletRepository;
    
    private UUID walletId;
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("1000.00");
    
    @BeforeEach
    void setUp() {
        walletId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
            .id(walletId)
            .balance(INITIAL_BALANCE)
            .version(0L)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        walletRepository.save(wallet);
    }
    
    @Test
    void testDeposit() {
        WalletOperationRequest request = new WalletOperationRequest(
            walletId,
            WalletOperationRequest.OperationType.DEPOSIT,
            new BigDecimal("500.00")
        );
        
        var response = walletService.processOperation(request);
        
        assertEquals(walletId, response.getWalletId());
        assertEquals(new BigDecimal("1500.00"), response.getBalance());
        assertEquals("DEPOSIT", response.getOperationType());
        
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("1500.00"), wallet.getBalance());
    }
    
    @Test
    void testWithdraw() {
        WalletOperationRequest request = new WalletOperationRequest(
            walletId,
            WalletOperationRequest.OperationType.WITHDRAW,
            new BigDecimal("300.00")
        );
        
        var response = walletService.processOperation(request);
        
        assertEquals(walletId, response.getWalletId());
        assertEquals(new BigDecimal("700.00"), response.getBalance());
        assertEquals("WITHDRAW", response.getOperationType());
        
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(new BigDecimal("700.00"), wallet.getBalance());
    }
    
    @Test
    void testWithdrawInsufficientFunds() {
        WalletOperationRequest request = new WalletOperationRequest(
            walletId,
            WalletOperationRequest.OperationType.WITHDRAW,
            new BigDecimal("2000.00")
        );
        
        assertThrows(InsufficientFundsException.class, () -> {
            walletService.processOperation(request);
        });
        
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(INITIAL_BALANCE, wallet.getBalance());
    }
    
    @Test
    void testWalletNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        WalletOperationRequest request = new WalletOperationRequest(
            nonExistentId,
            WalletOperationRequest.OperationType.DEPOSIT,
            new BigDecimal("100.00")
        );
        
        assertThrows(WalletNotFoundException.class, () -> {
            walletService.processOperation(request);
        });
    }
    
    @Test
    void testGetWalletBalance() {
        var response = walletService.getWalletBalance(walletId);
        
        assertEquals(walletId, response.getWalletId());
        assertEquals(INITIAL_BALANCE, response.getBalance());
    }
    
    @Test
    void testGetWalletBalanceNotFound() {
        UUID nonExistentId = UUID.randomUUID();
        
        assertThrows(WalletNotFoundException.class, () -> {
            walletService.getWalletBalance(nonExistentId);
        });
    }
    
    @Test
    void testConcurrentDeposits() throws InterruptedException {
        int threadCount = 100;
        int operationsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        WalletOperationRequest request = new WalletOperationRequest(
                            walletId,
                            WalletOperationRequest.OperationType.DEPOSIT,
                            new BigDecimal("1.00")
                        );
                        walletService.processOperation(request);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        BigDecimal expectedBalance = INITIAL_BALANCE
            .add(new BigDecimal(successCount.get()));
        
        assertEquals(expectedBalance, wallet.getBalance());
        assertEquals(0, errorCount.get());
    }
    
    @Test
    void testConcurrentWithdraws() throws InterruptedException {
        // Set up wallet with sufficient balance
        Wallet wallet = walletRepository.findById(walletId).orElseThrow();
        wallet.setBalance(new BigDecimal("10000.00"));
        walletRepository.save(wallet);
        
        int threadCount = 50;
        int operationsPerThread = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    for (int j = 0; j < operationsPerThread; j++) {
                        WalletOperationRequest request = new WalletOperationRequest(
                            walletId,
                            WalletOperationRequest.OperationType.WITHDRAW,
                            new BigDecimal("1.00")
                        );
                        walletService.processOperation(request);
                        successCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }
        
        latch.await();
        executor.shutdown();
        
        wallet = walletRepository.findById(walletId).orElseThrow();
        BigDecimal expectedBalance = new BigDecimal("10000.00")
            .subtract(new BigDecimal(successCount.get()));
        
        assertEquals(expectedBalance, wallet.getBalance());
        assertEquals(0, errorCount.get());
    }
}