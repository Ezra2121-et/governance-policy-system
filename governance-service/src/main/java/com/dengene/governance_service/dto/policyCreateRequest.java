package com.dengene.governance_service.dto;

import lombok.Data;

@Data
public class policyCreateRequest {
    private String title;
    private String description;
    private String createdBy;
}