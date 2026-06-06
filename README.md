# Governance Policy Management System

A backend system that manages governance policies with event-driven audit logging using Spring Boot, Kafka, and PostgreSQL.

## Architecture

The system consists of two microservices:

- **Governance Service** (Port 8080): Manages policy lifecycle and publishes events to Kafka
- **Audit Service** (Port 8081): Consumes events and maintains immutable audit logs

### Technology Stack

- Java 21
- Spring Boot 3.5.0
- Apache Kafka (Event streaming)
- PostgreSQL (Data persistence)
- Maven (Build tool)
- Docker Compose (Local development)

## Getting Started

### Prerequisites

- Docker and Docker Compose
- Java 21+
- Maven 3.9+

### Running the System

1. Start the infrastructure (PostgreSQL, Kafka, Zookeeper):
```bash
docker compose up -d
```

2. Build both services:
```bash
mvn clean install
```

3. Run Governance Service:
```bash
cd governance-service
mvn spring-boot:run
```

4. In another terminal, run Audit Service:
```bash
cd audit-service
mvn spring-boot:run
```

### API Endpoints

#### Governance Service (http://localhost:8080)

- `POST /policies` - Create a new policy
- `GET /policies` - Get all policies
- `GET /policies/{id}` - Get policy by ID
- `POST /policies/{id}/submit` - Submit policy for approval
- `POST /policies/{id}/approve` - Approve a policy
- `POST /policies/{id}/reject` - Reject a policy

#### Audit Service (http://localhost:8081)

- `GET /audit-logs` - Get all audit logs

### Example Requests

Create a policy:
```bash
curl -X POST http://localhost:8080/policies \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Data Retention Policy",
    "description": "All data must be deleted after two years",
    "createdBy": "admin"
  }'
```

Submit for approval:
```bash
curl -X POST http://localhost:8080/policies/1/submit
```

Approve:
```bash
curl -X POST http://localhost:8080/policies/1/approve
```

View audit logs:
```bash
curl http://localhost:8081/audit-logs
```

### Database Schemas

#### Governance Service (governance_db)
- `policies` table with fields: id, title, description, status, created_by, created_at

#### Audit Service (audit_db)
- `audit_logs` table with fields: id, event_type, policy_id, actor, timestamp

### Design Decisions

1. **Separate Databases**: Each service maintains its own database schema following microservice best practices
2. **Event-Driven**: Policy actions trigger Kafka events for asynchronous processing
3. **Immutable Logs**: Audit logs are only inserted, never updated, ensuring traceability
4. **Spring Data JPA**: Simplifies database operations without raw SQL
