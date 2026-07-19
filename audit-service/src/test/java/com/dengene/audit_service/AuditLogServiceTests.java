package com.dengene.audit_service;

import com.dengene.audit_service.model.AuditLog;
import com.dengene.audit_service.repository.AuditLogRepository;
import com.dengene.audit_service.service.AuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTests {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogService auditLogService;

    private AuditLog mockAuditLog;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
        mockAuditLog = new AuditLog();
        mockAuditLog.setId(1L);
        mockAuditLog.setEventType("policy-created");
        mockAuditLog.setPolicyId(1L);
        mockAuditLog.setActor("admin");
        mockAuditLog.setTimestamp(now);
    }

    @Test
    void logEvent_ShouldSaveAuditLogWithCorrectFields() {
        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(mockAuditLog);

        AuditLog result = auditLogService.logEvent("policy-created", 1L, "admin", now);

        assertNotNull(result);
        assertEquals("policy-created", result.getEventType());
        assertEquals(1L, result.getPolicyId());
        assertEquals("admin", result.getActor());
        assertEquals(now, result.getTimestamp());
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void logEvent_ShouldPersistDifferentEventTypes() {
        AuditLog approvedLog = new AuditLog();
        approvedLog.setId(2L);
        approvedLog.setEventType("policy-approved");
        approvedLog.setPolicyId(5L);
        approvedLog.setActor("reviewer");
        approvedLog.setTimestamp(now);

        when(auditLogRepository.save(any(AuditLog.class))).thenReturn(approvedLog);

        AuditLog result = auditLogService.logEvent("policy-approved", 5L, "reviewer", now);

        assertEquals("policy-approved", result.getEventType());
        assertEquals(5L, result.getPolicyId());
        assertEquals("reviewer", result.getActor());
    }

    @Test
    void getAllAuditLogs_ShouldReturnAllLogs() {
        AuditLog secondLog = new AuditLog();
        secondLog.setId(2L);
        secondLog.setEventType("policy-submitted");
        secondLog.setPolicyId(1L);
        secondLog.setActor("admin");
        secondLog.setTimestamp(now);

        when(auditLogRepository.findAll()).thenReturn(List.of(mockAuditLog, secondLog));

        List<AuditLog> results = auditLogService.getAllAuditLogs();

        assertEquals(2, results.size());
        verify(auditLogRepository).findAll();
    }

    @Test
    void getAllAuditLogs_WhenEmpty_ShouldReturnEmptyList() {
        when(auditLogRepository.findAll()).thenReturn(List.of());

        List<AuditLog> results = auditLogService.getAllAuditLogs();

        assertTrue(results.isEmpty());
        verify(auditLogRepository).findAll();
    }
}
