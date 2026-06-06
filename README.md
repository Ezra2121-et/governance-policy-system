# Governance Policy Management System
A backend system that manages governance policies with event-driven audit logging.
## Tech Stack
- Java 21 · Spring Boot 3.5.0 · Apache Kafka · PostgreSQL · Docker Compose
## Architecture
```
Client → Governance Service (8080) → PostgreSQL (governance_db)
                    ↓
                  Kafka
                    ↓
         Audit Service (8081) → PostgreSQL (audit_db)
```
## Policy Lifecycle
```
DRAFT → PENDING_APPROVAL → APPROVED
                        → REJECTED
```
## Getting Started
### Prerequisites
- Docker and Docker Compose
- Java 21+
- Maven 3.9+
### Run the System
1. Start infrastructure:
```
docker compose up -d
```
2. Start Governance Service (Terminal 1):
```
cd governance-service
./mvnw spring-boot:run
```
3. Start Audit Service (Terminal 2):
```
cd audit-service
./mvnw spring-boot:run
```
## API Endpoints
### Governance Service — http://localhost:8080
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /policies | Create a new policy |
| GET | /policies | Get all policies |
| GET | /policies/{id} | Get policy by ID |
| POST | /policies/{id}/submit | Submit for approval |
| POST | /policies/{id}/approve | Approve a policy |
| POST | /policies/{id}/reject | Reject a policy |
### Audit Service — http://localhost:8081
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /audit-logs | Get all audit logs |
## Swagger UI
- Governance Service: http://localhost:8080/swagger-ui.html
- Audit Service: http://localhost:8081/swagger-ui.html
## Example Usage
```
# Create a policy
curl -X POST http://localhost:8080/policies \
  -H "Content-Type: application/json" \
  -d '{"title": "Data Retention Policy", "description": "All data deleted after 2 years", "createdBy": "admin"}'
# Submit → Approve
curl -X POST http://localhost:8080/policies/1/submit
curl -X POST http://localhost:8080/policies/1/approve
# View audit trail
curl http://localhost:8081/audit-logs
```
## Running Tests
```
cd governance-service
mvn test
```
## Database Schema
**governance_db** — `policies`: id, title, description, status, created_by, created_at
**audit_db** — `audit_logs`: id, event_type, policy_id, actor, timestamp
## Design Decisions
- **Database per service** — each service owns its own schema
- **Event-driven** — Kafka decouples the two services asynchronously
- **Immutable audit logs** — only inserts, never updates