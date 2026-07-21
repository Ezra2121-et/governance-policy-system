package com.dengene.governance_service.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String eventType;
    private Long policyId;
    private String actor;
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    private LocalDateTime publishedAt;

    public OutboxEvent(String eventType, Long policyId, String actor) {
        this.eventType = eventType;
        this.policyId = policyId;
        this.actor = actor;
        this.createdAt = LocalDateTime.now();
        this.status = OutboxStatus.PENDING;
    }
}