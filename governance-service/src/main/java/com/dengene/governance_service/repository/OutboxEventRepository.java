package com.dengene.governance_service.repository;

import com.dengene.governance_service.model.OutboxEvent;
import com.dengene.governance_service.model.OutboxStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    List<OutboxEvent> findByStatus(OutboxStatus status);
}