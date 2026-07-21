package com.dengene.audit_service.controller;

import com.dengene.audit_service.dto.AuditConfirmRequest;
import com.dengene.audit_service.dto.AuditLogResponse;
import com.dengene.audit_service.model.AuditLog;
import com.dengene.audit_service.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/audit-logs")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getAllAuditLogs() {
        List<AuditLog> logs = auditLogService.getAllAuditLogs();
        List<AuditLogResponse> responses = logs.stream()
                .map(AuditLogResponse::fromAuditLog)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    // Synchronous write used by governance-service's Saga orchestration for
    // policy approval. Distinct from the async Kafka path — this is the
    // "step" a saga waits on to decide whether to proceed or compensate.
    @PostMapping("/confirm")
    public ResponseEntity<AuditLogResponse> confirmAuditLog(@RequestBody AuditConfirmRequest request) {
        AuditLog savedLog = auditLogService.logEvent(
                request.getEventType(), request.getPolicyId(), request.getActor(), LocalDateTime.now());
        return ResponseEntity.status(HttpStatus.CREATED).body(AuditLogResponse.fromAuditLog(savedLog));
    }
}