# Wallet Service

A high-performance wallet service built with Spring Boot 3 that handles deposit and withdraw operations with concurrency guarantees.

## Features

- RESTful API for wallet operations (DEPOSIT/WITHDRAW)
- Get wallet balance by UUID
- Optimistic locking with retry mechanism for concurrent operations
- Pessimistic locking at database level for write operations
- Comprehensive error handling (wallet not found, insufficient funds, invalid JSON)
- Database migrations using Liquibase
- Docker and docker-compose support
- Environment-based configuration
- Comprehensive test coverage

## Tech Stack

- Java 17
- Spring Boot 3.2.0
- PostgreSQL 15
- Liquibase
- Docker & Docker Compose
- Testcontainers for integration tests

## Example of usage

### Create a wallet (manually)

```sql
INSERT INTO wallets (id, balance, version, created_at, updated_at)
VALUES ('550e8400-e29b-41d4-a716-446655440000', 1000.00, 0, NOW(), NOW());
```

### Deposit money

```bash
curl -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{
    "walletId": "550e8400-e29b-41d4-a716-446655440000",
    "operationType": "DEPOSIT",
    "amount": 500.00
  }'
```

### Withdraw money

```bash
curl -X POST http://localhost:8080/api/v1/wallet \
  -H "Content-Type: application/json" \
  -d '{
    "walletId": "550e8400-e29b-41d4-a716-446655440000",
    "operationType": "WITHDRAW",
    "amount": 200.00
  }'
```

### Get balance

```bash
curl http://localhost:8080/api/v1/wallets/550e8400-e29b-41d4-a716-446655440000
```
