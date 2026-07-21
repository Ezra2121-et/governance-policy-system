package com.dengene.audit_service.dto;

import lombok.Data;

@Data
public class AuditConfirmRequest {
    private String eventType;
    private Long policyId;
    private String actor;
}