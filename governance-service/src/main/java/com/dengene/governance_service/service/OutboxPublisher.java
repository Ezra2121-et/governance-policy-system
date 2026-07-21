package com.dengene.governance_service.service;

import com.dengene.governance_service.model.OutboxEvent;
import com.dengene.governance_service.model.OutboxStatus;
import com.dengene.governance_service.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.util.List;

// Polls the outbox table on a fixed interval and publishes any pending
// events to Kafka. This decouples "did the DB write succeed" from "did the
// Kafka publish succeed" — if Kafka is down, events just wait here instead
// of being lost.
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaProducerService kafkaProducerService;

    @Scheduled(fixedDelay = 3000) // check every 3 seconds
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findByStatus(OutboxStatus.PENDING);

        for (OutboxEvent event : pending) {
            try {
                kafkaProducerService.publishEvent(event.getEventType(), event.getPolicyId(), event.getActor());
                event.setStatus(OutboxStatus.PUBLISHED);
                event.setPublishedAt(LocalDateTime.now());
                outboxEventRepository.save(event);
                log.info("Published outbox event id={} type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                log.error("Failed to publish outbox event id={}, will retry next cycle", event.getId(), e);
                // deliberately left as PENDING — next scheduled run retries it
            }
        }
    }
}