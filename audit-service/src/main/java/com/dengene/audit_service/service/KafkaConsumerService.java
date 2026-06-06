package com.dengene.audit_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaConsumerService {

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "governance-events", groupId = "audit-group")
    public void consumeEvent(String message) {
        try {
            Map<String, Object> event = objectMapper.readValue(message, Map.class);

            String eventType = (String) event.get("eventType");
            Long policyId = ((Number) event.get("policyId")).longValue();
            String actor = (String) event.get("actor");
            LocalDateTime timestamp = LocalDateTime.parse((String) event.get("timestamp"));

            auditLogService.logEvent(eventType, policyId, actor, timestamp);

            log.info("Consumed event: {} for policy: {}", eventType, policyId);
        } catch (Exception e) {
            log.error("Failed to consume event", e);
        }
    }
}