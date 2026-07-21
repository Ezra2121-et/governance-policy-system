package com.dengene.governance_service.service;

import com.dengene.governance_service.model.Policy;
import com.dengene.governance_service.model.PolicyStatus;
import com.dengene.governance_service.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

// Orchestrates approving a policy as a two-step saga:
//   1. Locally set the policy to APPROVED.
//   2. Synchronously confirm the write with audit-service.
// If step 2 fails, we run a compensating action: revert the policy back to
// PENDING_APPROVAL, so the system never ends up "approved" with no audit
// record of it having happened.
@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyApprovalSagaService {

    private final PolicyRepository policyRepository;
    private final RestTemplate restTemplate;

    public Policy approveWithSaga(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found with id: " + policyId));

        if (!policy.getStatus().equals(PolicyStatus.PENDING_APPROVAL)) {
            throw new RuntimeException("Only PENDING_APPROVAL policies can be approved");
        }

        // Step 1: local state change
        PolicyStatus previousStatus = policy.getStatus();
        policy.setStatus(PolicyStatus.APPROVED);
        policyRepository.save(policy);
        log.info("Saga step 1: policy {} set to APPROVED locally", policyId);

        // Step 2: synchronous confirmation with audit-service
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("eventType", "policy-approved");
            body.put("policyId", policyId);
            body.put("actor", policy.getCreatedBy());

            restTemplate.postForEntity("http://audit-service/audit-logs/confirm", body, String.class);
            log.info("Saga step 2: audit-service confirmed policy {}", policyId);

        } catch (Exception e) {
            log.error("Saga step 2 FAILED for policy {} — compensating by reverting status", policyId, e);

            // Compensating transaction: undo step 1
            policy.setStatus(previousStatus);
            policyRepository.save(policy);

            throw new RuntimeException(
                    "Policy approval failed: audit-service confirmation unavailable. Change rolled back.", e);
        }

        return policy;
    }
}