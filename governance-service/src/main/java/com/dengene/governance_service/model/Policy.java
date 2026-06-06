package com.dengene.governance_service.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "policies")
public class Policy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private String createdBy;

    @Enumerated(EnumType.STRING)
    private PolicyStatus status;

    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.status = PolicyStatus.DRAFT;
        this.createdAt = LocalDateTime.now();
    }
}
