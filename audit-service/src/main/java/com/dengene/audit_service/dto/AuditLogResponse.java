package com.dengene.audit_service.dto;

import com.dengene.audit_service.model.AuditLog;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String eventType;
    private Long policyId;
    private String actor;
    private LocalDateTime timestamp;

    public static AuditLogResponse fromAuditLog(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getEventType(),
                auditLog.getPolicyId(),
                auditLog.getActor(),
                auditLog.getTimestamp()
        );
    }
}