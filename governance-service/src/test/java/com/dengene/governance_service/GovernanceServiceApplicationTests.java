package com.dengene.governance_service;

import com.dengene.governance_service.model.Policy;
import com.dengene.governance_service.model.PolicyStatus;
import com.dengene.governance_service.repository.PolicyRepository;
import com.dengene.governance_service.service.KafkaProducerService;
import com.dengene.governance_service.service.PolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GovernanceServiceApplicationTests {

	@Mock
	private PolicyRepository policyRepository;

	@Mock
	private KafkaProducerService kafkaProducerService;

	@InjectMocks
	private PolicyService policyService;

	private Policy mockPolicy;

	@BeforeEach
	void setUp() {
		mockPolicy = new Policy();
		mockPolicy.setId(1L);
		mockPolicy.setTitle("Test Policy");
		mockPolicy.setDescription("Test Description");
		mockPolicy.setCreatedBy("admin");
		mockPolicy.setStatus(PolicyStatus.DRAFT);
		mockPolicy.setCreatedAt(LocalDateTime.now());
	}

	@Test
	void createPolicy_ShouldReturnPolicyWithDraftStatus() {
		when(policyRepository.save(any(Policy.class))).thenReturn(mockPolicy);

		Policy result = policyService.createPolicy("Test Policy", "Test Description", "admin");

		assertNotNull(result);
		assertEquals(PolicyStatus.DRAFT, result.getStatus());
		assertEquals("Test Policy", result.getTitle());
		verify(kafkaProducerService).publishEvent("policy-created", 1L, "admin");
	}

	@Test
	void submitPolicy_ShouldChangeToPendingApproval() {
		when(policyRepository.findById(1L)).thenReturn(Optional.of(mockPolicy));
		when(policyRepository.save(any(Policy.class))).thenReturn(mockPolicy);

		Policy result = policyService.submitPolicy(1L);

		assertEquals(PolicyStatus.PENDING_APPROVAL, result.getStatus());
		verify(kafkaProducerService).publishEvent("policy-submitted", 1L, "admin");
	}

	@Test
	void approvePolicy_ShouldChangeToApproved() {
		mockPolicy.setStatus(PolicyStatus.PENDING_APPROVAL);
		when(policyRepository.findById(1L)).thenReturn(Optional.of(mockPolicy));
		when(policyRepository.save(any(Policy.class))).thenReturn(mockPolicy);

		Policy result = policyService.approvePolicy(1L);

		assertEquals(PolicyStatus.APPROVED, result.getStatus());
		verify(kafkaProducerService).publishEvent("policy-approved", 1L, "admin");
	}

	@Test
	void rejectPolicy_ShouldChangeToRejected() {
		mockPolicy.setStatus(PolicyStatus.PENDING_APPROVAL);
		when(policyRepository.findById(1L)).thenReturn(Optional.of(mockPolicy));
		when(policyRepository.save(any(Policy.class))).thenReturn(mockPolicy);

		Policy result = policyService.rejectPolicy(1L);

		assertEquals(PolicyStatus.REJECTED, result.getStatus());
		verify(kafkaProducerService).publishEvent("policy-rejected", 1L, "admin");
	}

	@Test
	void submitPolicy_WhenNotDraft_ShouldThrowException() {
		mockPolicy.setStatus(PolicyStatus.APPROVED);
		when(policyRepository.findById(1L)).thenReturn(Optional.of(mockPolicy));

		assertThrows(RuntimeException.class, () -> policyService.submitPolicy(1L));
	}

	@Test
	void approvePolicy_WhenNotPendingApproval_ShouldThrowException() {
		mockPolicy.setStatus(PolicyStatus.DRAFT);
		when(policyRepository.findById(1L)).thenReturn(Optional.of(mockPolicy));

		assertThrows(RuntimeException.class, () -> policyService.approvePolicy(1L));
	}
}