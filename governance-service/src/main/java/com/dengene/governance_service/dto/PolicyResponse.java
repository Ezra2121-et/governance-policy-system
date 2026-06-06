package com.dengene.governance_service.dto;

import com.dengene.governance_service.model.Policy;
import com.dengene.governance_service.model.PolicyStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PolicyResponse {
    private Long id;
    private String title;
    private String description;
    private PolicyStatus status;
    private String createdBy;
    private LocalDateTime createdAt;

    public static PolicyResponse fromPolicy(Policy policy) {
        return new PolicyResponse(
                policy.getId(),
                policy.getTitle(),
                policy.getDescription(),
                policy.getStatus(),
                policy.getCreatedBy(),
                policy.getCreatedAt()
        );
    }
}