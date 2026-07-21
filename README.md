# Governance Policy Management System
A backend system that manages governance policies with event-driven audit logging, fronted by an API Gateway with service discovery, JWT auth, circuit breaking, and rate limiting.

## Tech Stack
- Java 21 · Spring Boot 3.5.0 · Spring Cloud Gateway · Spring Cloud Netflix Eureka
- Apache Kafka · gRPC · Redis · PostgreSQL · Docker Compose

## Architecture
```
Client
  │
  ▼
API Gateway (8082)
  │  - JWT auth
  │  - Redis rate limiting
  │  - Resilience4j circuit breaker
  │  - Eureka-based load-balanced routing
  │
  ├──► Governance Service (8080, +8083 when scaled) → PostgreSQL (governance_db)
  │        │
  │        ├─► Outbox table → scheduled publisher → Kafka → Audit Service (async)
  │        └─► gRPC client (9091) → Audit Service (sync, used by approval saga)
  │
  └──► Audit Service (8081, gRPC 9091) → PostgreSQL (audit_db)

Eureka Server (8761) — all four services register here
```

See [docs/round2-writeup.md](docs/round2-writeup.md) for the full design write-up: API Gateway design, REST vs gRPC comparison, and the Outbox/Saga pattern explanations.

## Policy Lifecycle
```
DRAFT → PENDING_APPROVAL → APPROVED
                        → REJECTED
```
Approving a policy runs as an orchestrated **Saga**: the policy status is set locally, then synchronously confirmed with Audit Service via gRPC. If confirmation fails, the status change is rolled back (compensating transaction).

## Getting Started
### Prerequisites
- Docker and Docker Compose
- Java 21+
- Maven 3.9+

### Run the System
1. Start infrastructure (Postgres x2, Kafka, Zookeeper, Redis):
```
docker compose up -d
```
2. Start Eureka Server (Terminal 1):
```
cd eureka-server
./mvnw spring-boot:run
```
3. Start Governance Service (Terminal 2):
```
cd governance-service
./mvnw spring-boot:run
```
4. Start Audit Service (Terminal 3):
```
cd audit-service
./mvnw spring-boot:run
```
5. Start API Gateway (Terminal 4):
```
cd api-gateway
./mvnw spring-boot:run
```
6. Check everything registered: open `http://localhost:8761` — you should see `GOVERNANCE-SERVICE`, `AUDIT-SERVICE`, and `API-GATEWAY` listed as `UP`.

## Authentication
All Gateway routes require a JWT. Get one first:
```
curl -X POST "http://localhost:8082/auth/token?username=yourname"
```
Then include it on every request:
```
-H "Authorization: Bearer <token>"
```

## API Endpoints (via API Gateway — http://localhost:8082)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /policies | Create a new policy |
| GET | /policies | Get all policies |
| GET | /policies/{id} | Get policy by ID |
| POST | /policies/{id}/submit | Submit for approval |
| POST | /policies/{id}/approve | Approve a policy (runs the Saga) |
| POST | /policies/{id}/reject | Reject a policy |
| GET | /audit-logs | Get all audit logs |

Direct service ports (8080, 8081) still work for local debugging, but the Gateway (8082) is the intended entry point — it's the only path with auth, rate limiting, and circuit breaking applied.

## Swagger UI
- Governance Service: http://localhost:8080/swagger-ui.html
- Audit Service: http://localhost:8081/swagger-ui.html

## Example Usage
```
TOKEN=$(curl -s -X POST "http://localhost:8082/auth/token?username=admin")

# Create a policy
curl -X POST http://localhost:8082/policies \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"title": "Data Retention Policy", "description": "All data deleted after 2 years", "createdBy": "admin"}'

# Submit → Approve (approve triggers the gRPC-backed saga)
curl -X POST http://localhost:8082/policies/1/submit -H "Authorization: Bearer $TOKEN"
curl -X POST http://localhost:8082/policies/1/approve -H "Authorization: Bearer $TOKEN"

# View audit trail
curl http://localhost:8082/audit-logs -H "Authorization: Bearer $TOKEN"
```

## Running Tests
```
cd governance-service
mvn test

cd audit-service
mvn test
```

## Database Schema
**governance_db**
- `policies`: id, title, description, status, created_by, created_at
- `outbox_events`: id, event_type, policy_id, actor, status, created_at, published_at

**audit_db**
- `audit_logs`: id, event_type, policy_id, actor, timestamp

## Design Decisions
- **Database per service** — each service owns its own schema
- **Event-driven (async)** — Kafka, via the Outbox pattern, decouples policy creation/submission/rejection from the audit trail without risking a dual-write gap
- **Synchronous confirmation for approval** — approving a policy is business-critical enough to need an immediate guarantee, handled via a gRPC-based Saga with compensation instead of eventual consistency
- **Immutable audit logs** — only inserts, never updates
- **Gateway as the single entry point** — auth, rate limiting, and circuit breaking live in one place instead of being duplicated across services
- **REST for external traffic, gRPC for internal service-to-service calls** — see [docs/round2-writeup.md](docs/round2-writeup.md) for the full reasoning
