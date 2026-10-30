# Partie I — Réponses DevOps

## Secrets vs configuration

- **Configuration** : paramètres non sensibles (ports, URLs Eureka, `bank.transaction.max-amount`, feature flags). Versionnés dans Config Server / `application.yml`.
- **Secrets** : credentials (DB, OpenAI, Telegram, Keycloak). Jamais dans Git. Injectés via variables d'environnement, Docker secrets, ou un vault (HashiCorp / cloud secret manager). Distingués dans CI via `secrets.*` GitHub Actions.

## Contrôles de démarrage, disponibilité et vitalité

- **Liveness** (`/actuator/health/liveness`) : le process vit-il ? Sinon redémarrage du conteneur.
- **Readiness** (`/actuator/health/readiness`) : prêt à recevoir du trafic (DB/Kafka up) ? Sinon retiré du load balancer.
- **Startup** : sonde pendant le boot JVM (évite les kills prématurés).

## Logs, métriques et traces distribuées

- **Logs** : JSON structuré (correlation-id / traceId) vers stdout → agrégateur (ELK/Loki).
- **Métriques** : Micrometer → Prometheus/Grafana (latence, erreurs, circuit-breaker states).
- **Traces** : OpenTelemetry / Micrometer Tracing → Jaeger/Zipkin pour suivre une requête Gateway → Feign → Kafka.

## Stratégie de rollback

1. Images taguées par git SHA dans le registre.
2. En cas d'échec post-déploiement : `docker compose` / orchestrateur revient au SHA précédent.
3. Config Server : rollback du fichier de config (max-amount) sans rebuild.
4. Migrations DB backward-compatible (expand/contract) pour permettre un revert applicatif sans perte de données.
