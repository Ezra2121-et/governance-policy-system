package com.dengene.audit_service.controller;

import com.dengene.audit_service.dto.AuditLogResponse;
import com.dengene.audit_service.model.AuditLog;
import com.dengene.audit_service.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}