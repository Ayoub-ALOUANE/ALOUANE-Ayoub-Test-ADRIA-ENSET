# Partie B — Conception et Architecture

## 1. Schéma d'architecture

```mermaid
flowchart TB
  subgraph Clients
    WEB[React Frontend :4200]
    REST_CLIENT[REST / Telegram]
  end

  GW[Spring Cloud Gateway :8080]
  EU[Eureka Discovery :8761]
  CFG[Config Server :8888]

  ACC[account-service :8081]
  TX[transaction-service :8082]
  NOTIF[notification-service :8083]
  AI[ai-assistant :8084]
  KAFKA[(Kafka topic\nbank.transactions.created)]

  WEB --> GW
  REST_CLIENT --> GW
  REST_CLIENT --> AI

  GW --> ACC
  GW --> TX
  GW --> AI

  ACC -.->|register| EU
  TX -.->|register| EU
  AI -.->|register| EU
  NOTIF -.->|register| EU
  GW -.->|register| EU
  CFG -.->|register| EU

  TX -->|OpenFeign + Resilience4J| ACC
  TX -->|publish| KAFKA
  NOTIF -->|consume / journal| KAFKA

  AI -->|MCP tools HTTP| ACC
  AI -->|MCP tools HTTP| TX

  ACC -.->|optional:config| CFG
  TX -.->|bank.transaction.max-amount| CFG
```

## 2. Diagramme de classes (domaine métier)

```mermaid
classDiagram
  class Account {
    UUID id
    String customerReference
    AccountStatus status
    String currency
    Double balance
  }
  class AccountStatus {
    <<enumeration>>
    ACTIVE
    BLOCKED
  }
  class Transaction {
    UUID id
    UUID accountId
    BigDecimal amount
    String currency
    String merchantCategory
    String country
    Instant createdAt
  }
  class TransactionCreatedEvent {
    UUID eventId
    String eventType
    UUID transactionId
    UUID accountId
    BigDecimal amount
    String currency
    Instant occurredAt
  }
  class AccountService {
    create()
    getById()
    updateBalance()
    transfer()
  }
  class TransactionService {
    create()
    transfer()
    getByAccount()
  }
  class AccountClient {
    <<Feign>>
    getAccount()
    updateBalance()
    transfer()
  }

  Account --> AccountStatus
  AccountService --> Account
  TransactionService --> Transaction
  TransactionService --> AccountClient
  TransactionService --> TransactionCreatedEvent
  AccountClient ..> AccountService : HTTP
```

## 3. Couches par microservice

```
account-service / transaction-service
├── domain/
├── repository/
├── service/
├── web/rest/
├── web/graphql/
└── mcp/
```

REST, GraphQL et MCP appellent **la même couche service**.
