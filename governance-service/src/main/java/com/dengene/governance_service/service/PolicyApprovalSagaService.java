package com.dengene.governance_service.service;

import com.dengene.governance_service.model.Policy;
import com.dengene.governance_service.model.PolicyStatus;
import com.dengene.governance_service.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PolicyApprovalSagaService {

    private final PolicyRepository policyRepository;
    private final AuditGrpcClient auditGrpcClient;

    public Policy approveWithSaga(Long policyId) {
        Policy policy = policyRepository.findById(policyId)
                .orElseThrow(() -> new RuntimeException("Policy not found with id: " + policyId));

        if (!policy.getStatus().equals(PolicyStatus.PENDING_APPROVAL)) {
            throw new RuntimeException("Only PENDING_APPROVAL policies can be approved");
        }

        PolicyStatus previousStatus = policy.getStatus();
        policy.setStatus(PolicyStatus.APPROVED);
        policyRepository.save(policy);
        log.info("Saga step 1: policy {} set to APPROVED locally", policyId);

        try {
            var reply = auditGrpcClient.confirmAuditLog("policy-approved", policyId, policy.getCreatedBy());
            log.info("Saga step 2 (gRPC): audit-service confirmed policy {} with id={}", policyId, reply.getId());

        } catch (Exception e) {
            log.error("Saga step 2 FAILED for policy {} — compensating by reverting status", policyId, e);
            policy.setStatus(previousStatus);
            policyRepository.save(policy);
            throw new RuntimeException(
                    "Policy approval failed: audit-service confirmation unavailable. Change rolled back.", e);
        }

        return policy;
    }
}