# Round 2: API Gateway & gRPC Enhancement — Design Document

**Author:** Yosef Getnet Damte
**Project:** governance-policy-system

---

## 1. Architecture Overview

### Before (Round 1)
Two independent Spring Boot services, each exposed directly to callers, with no shared entry point, no auth layer, and no protection against cascading failures:

```
Client → governance-service (:8080) → Postgres (governance_db)
Client → audit-service (:8081) → Postgres (audit_db)
governance-service → Kafka (governance-events) → audit-service (consumer)
```

### After (Round 2)
A single entry point now fronts both services. Routing is name-based via service discovery rather than hardcoded hosts/ports, and every request passes through auth, rate limiting, and failure isolation before it ever reaches a downstream service.

```
Client
  │
  ▼
API Gateway (:8082)
  │  - JWT validation (rejects unauthenticated requests before routing)
  │  - Redis-backed rate limiting (per-IP token bucket)
  │  - Resilience4j circuit breaker (per-route, with fallback responses)
  │  - Eureka-based load-balanced routing (lb://service-name)
  │
  ├──► governance-service (:8080, :8083 — 2 instances load-balanced)
  │       - Postgres (governance_db)
  │       - Outbox table → scheduled publisher → Kafka
  │       - gRPC client → audit-service (:9091) for saga confirmation
  │
  └──► audit-service (:8081)
          - Postgres (audit_db)
          - Kafka consumer (async event trail)
          - gRPC server (:9091) — synchronous confirmation endpoint
          - REST (:8081) — read endpoints + legacy confirm endpoint

Eureka Server (:8761) — service registry, all four Spring Boot apps register here
```

**Key shift from Round 1 → Round 2:** governance-service and audit-service no longer talk to each other only asynchronously via Kafka. They now also have a synchronous path (gRPC) used specifically for the policy-approval saga, where the system needs to know *immediately* whether the audit write succeeded, not just eventually.

---

## 2. API Gateway Design

**Technology:** Spring Cloud Gateway (WebFlux/reactive stack — chosen because the Gateway needs to handle high concurrent connection volume without blocking a thread per request).

**Routing:** Path-based, resolved through Eureka rather than static config:
- `/policies/**` → `lb://governance-service`
- `/audit-logs/**` → `lb://audit-service`

**Cross-cutting filters applied per route, in this order:**
1. **JWT Authentication** (`GlobalFilter`, order `-1`) — runs before routing; rejects with `401` if the `Authorization` header is missing or the token fails signature/expiry validation. Downstream services never see unauthenticated traffic.
2. **Rate Limiting** (`RequestRateLimiter`, Redis-backed) — token bucket per client IP, `replenishRate: 5`, `burstCapacity: 10`. Protects against a single client overwhelming the system.
3. **Circuit Breaker** (Resilience4j) — sliding window of 5 calls, 50% failure threshold trips the circuit for 10s, then allows 3 test calls in half-open state. On open circuit, requests get an instant fallback response instead of hanging on a dead downstream service.

**Load balancing:** Free by-product of Eureka + `lb://` URIs — when a service has multiple registered instances (tested with governance-service running on both :8080 and :8083), the Gateway round-robins between them automatically.

---

## 3. REST vs gRPC — Direct Comparison

Both protocols were implemented for the *same* operation in this system — writing an audit log entry — which allowed a direct, tested comparison rather than a purely theoretical one.

| | REST (`AuditLogController` → `/audit-logs/confirm`) | gRPC (`AuditGrpcServer` → `CreateAuditLog`) |
|---|---|---|
| **Contract** | Implicit — a `@RequestBody` DTO class, no formal schema shared between services | Explicit — `.proto` file is the single source of truth; both services compile the *same* contract |
| **Serialization** | JSON (text, human-readable, larger payload) | Protobuf (binary, compact, faster to serialize/deserialize) |
| **Transport** | HTTP/1.1 | HTTP/2 (multiplexing, header compression) |
| **Client code** | Hand-written `RestTemplate` call, manual URL string, no compile-time safety on the request/response shape | Generated `AuditServiceBlockingStub` — compiler catches field name/type mismatches before runtime |
| **Discoverability** | Needs external documentation (Swagger/OpenAPI) to know what's callable | Contract is self-describing; `.proto` file *is* the documentation |
| **Debuggability** | Readable in browser dev tools, `curl`, Postman — trivial to inspect | Requires a gRPC-aware tool (e.g. `grpcurl`, BloomRPC) — not human-readable on the wire |
| **Best fit here** | External-facing traffic through the Gateway (browsers, Postman, any HTTP client) — REST's universality matters more than raw speed for user-facing calls | Internal service-to-service calls where both ends are Java code we control, and we care about contract safety + lower latency (used for the saga's synchronous confirmation step) |

**Practical takeaway from building both:** REST was faster to get working end-to-end (no codegen step, no new server port to manage), but gRPC's generated stubs eliminated a class of bugs that's easy to hit with REST — e.g., typo'ing a JSON field name silently produces a `null` instead of a compile error. For a system where governance-service and audit-service are both under the same team's control, gRPC's contract-first approach is the stronger long-term choice for internal calls; REST remains the right choice for anything crossing the Gateway to external clients.

---

## 4. Design Pattern Answers

### 4.1 The Dual-Write Problem → Outbox Pattern

**Problem:** Writing to Postgres and publishing to Kafka are two separate operations against two separate systems with no shared transaction. If the DB write succeeds but the Kafka publish fails, the audit trail silently has a gap — a policy exists with no record of it ever happening.

**Solution implemented:** Instead of publishing to Kafka directly inside the request path, `PolicyService` writes to Postgres twice in the *same* `@Transactional` method — once to the `policies` table, once to a new `outbox_events` table. A separate `@Scheduled` poller (`OutboxPublisher`, running every 3 seconds) reads `PENDING` rows and publishes them to Kafka, marking them `PUBLISHED` on success. If Kafka is unreachable, the row simply stays `PENDING` and gets retried on the next cycle — nothing is lost.

**Verified working:** Tested by creating a policy and confirming the `outbox_events` row transitioned from `PENDING` → `PUBLISHED` within one poll cycle, with the corresponding entry showing up in `audit-service`'s Kafka-consumed audit log.

### 4.2 Distributed Transactions → Saga Pattern

**Problem:** Approving a policy is logically a single business operation, but it spans two services (governance-service's local state, and audit-service's confirmation) with no distributed transaction coordinator between them.

**Solution implemented:** An orchestrated saga in `PolicyApprovalSagaService`:
1. **Step 1:** Locally set the policy status to `APPROVED` and save.
2. **Step 2:** Synchronously call `audit-service` (via gRPC) to confirm the write.
3. **Compensating transaction:** If step 2 fails for any reason, the policy status is reverted back to its previous state (`PENDING_APPROVAL`) and the failure is surfaced to the caller. The system never ends up in a state where a policy shows `APPROVED` but has no corresponding confirmation.

**Verified working:** Tested the happy path (policy correctly reaches `APPROVED` after both steps succeed) end-to-end through the Gateway, using the gRPC path for step 2.

---

## 5. Summary of What Was Built

| Requirement | Status |
|---|---|
| API Gateway with path-based routing | Done, tested |
| JWT authentication at the Gateway | Done, tested (valid/invalid/missing token cases) |
| Service discovery via Eureka | Done, tested (all 4 services register) |
| Load balancing | Done, tested (2 governance-service instances, confirmed round-robin) |
| Circuit breaker with fallback | Done, tested (forced failure → fallback → recovery) |
| Redis-backed rate limiting | Done, tested (burst capacity exceeded → 429) |
| gRPC contract + at least one flow | Done, tested (audit confirmation via gRPC, used in the saga) |
| Outbox pattern (dual-write problem) | Done, tested |
| Saga pattern (distributed transaction) | Done, tested |
| REST vs gRPC comparison | This document, Section 3 |
| Architecture diagram | This document, Section 1 |
| API Gateway design doc | This document, Section 2 |
