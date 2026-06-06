package com.dengene.governance_service.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void publishEvent(String eventType, Long policyId, String actor) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("eventType", eventType);
            event.put("policyId", policyId);
            event.put("actor", actor);
            event.put("timestamp", LocalDateTime.now());

            String eventJson = objectMapper.writeValueAsString(event);
            kafkaTemplate.send("governance-events", eventJson);

            log.info("Published event: {} for policy: {}", eventType, policyId);
        } catch (Exception e) {
            log.error("Failed to publish event", e);
            throw new RuntimeException("Failed to publish event", e);
        }
    }
}