package com.dengene.audit_service.service;

import com.dengene.audit_service.model.AuditLog;
import com.dengene.audit_service.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLog logEvent(String eventType, Long policyId, String actor, LocalDateTime timestamp) {
        AuditLog auditLog = new AuditLog();
        auditLog.setEventType(eventType);
        auditLog.setPolicyId(policyId);
        auditLog.setActor(actor);
        auditLog.setTimestamp(timestamp);

        return auditLogRepository.save(auditLog);
    }

    public List<AuditLog> getAllAuditLogs() {
        return auditLogRepository.findAll();
    }
}