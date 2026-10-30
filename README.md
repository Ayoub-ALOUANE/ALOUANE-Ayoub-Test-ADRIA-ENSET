# ALOUANE-Ayoub-Test-ADRIA-ENSET

Plateforme bancaire microservices — **Test technique ADRIA / ENSET**  
Architecture Spring Cloud · Kafka · MCP/AI · React · Fraud ML (Python)

## Modules Maven

| Module | Port | Rôle |
|---|---|---|
| `discovery-service` | 8761 | Eureka |
| `config-server` | 8888 | Config (`bank.transaction.max-amount=1000`) |
| `gateway-service` | 8080 | API Gateway |
| `account-service` | 8081 | Comptes (REST / GraphQL / MCP) |
| `transaction-service` | 8082 | Transactions + Feign + Resilience4J + Kafka |
| `notification-service` | 8083 | Consumer Kafka (journal) |
| `ai-assistant` | 8084 | Agent conversationnel MCP |
| `frontend/` | 4200 | Client React (Vite) |
| `fraud-detection/` | — | Classification fraude Python |

## Démarrage rapide (Docker)

```bash
docker compose up --build
```

- UI : http://localhost:4200  
- Gateway : http://localhost:8080  
- Eureka : http://localhost:8761  

## Démarrage local (sans Kafka)

```bash
mvn -pl discovery-service,config-server,account-service,gateway-service,ai-assistant -am spring-boot:run
# autre terminal
ACCOUNT_SERVICE_URL=http://localhost:8081 KAFKA_ENABLED=false \
  mvn -pl transaction-service spring-boot:run
cd frontend && npm run dev
```

## Exemples d'API

```bash
# Comptes
curl http://localhost:8081/api/accounts

# Transaction
curl -X POST http://localhost:8082/api/transactions \
  -H "Content-Type: application/json" \
  -d '{"accountId":"<UUID>","amount":25.5,"currency":"EUR","merchantCategory":"GROCERY","country":"FR"}'

# Chat agent
curl -X POST http://localhost:8084/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"list accounts"}'

# MCP tools
curl http://localhost:8081/mcp/tools
```

## Fraude ML

```bash
pip install -r fraud-detection/requirements.txt
python fraud-detection/fraud_detection.py
```

## Documentation

- Rapport : [`docs/RAPPORT.md`](docs/RAPPORT.md) (exporter en PDF)
- Architecture : [`docs/architecture.md`](docs/architecture.md)
- DevOps : [`docs/devops-answers.md`](docs/devops-answers.md)
- Sujet : [`test.md`](test.md)

## CI/CD

Pipeline GitHub Actions : `.github/workflows/ci-cd.yml`  
(compile → tests → images GHCR → deploy schématique)
