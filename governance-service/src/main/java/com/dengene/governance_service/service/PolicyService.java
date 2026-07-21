package com.dengene.governance_service.service;

import com.dengene.governance_service.model.OutboxEvent;
import com.dengene.governance_service.model.Policy;
import com.dengene.governance_service.model.PolicyStatus;
import com.dengene.governance_service.repository.OutboxEventRepository;
import com.dengene.governance_service.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public Policy createPolicy(String title, String description, String createdBy) {
        Policy policy = new Policy();
        policy.setTitle(title);
        policy.setDescription(description);
        policy.setCreatedBy(createdBy);

        Policy savedPolicy = policyRepository.save(policy);
        outboxEventRepository.save(new OutboxEvent("policy-created", savedPolicy.getId(), createdBy));

        return savedPolicy;
    }

    public List<Policy> getAllPolicies() {
        return policyRepository.findAll();
    }

    public Policy getPolicyById(Long id) {
        return policyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Policy not found with id: " + id));
    }

    @Transactional
    public Policy submitPolicy(Long id) {
        Policy policy = getPolicyById(id);
        if (!policy.getStatus().equals(PolicyStatus.DRAFT)) {
            throw new RuntimeException("Only DRAFT policies can be submitted");
        }
        policy.setStatus(PolicyStatus.PENDING_APPROVAL);
        Policy savedPolicy = policyRepository.save(policy);
        outboxEventRepository.save(new OutboxEvent("policy-submitted", policy.getId(), policy.getCreatedBy()));

        return savedPolicy;
    }

    @Transactional
    public Policy approvePolicy(Long id) {
        Policy policy = getPolicyById(id);
        if (!policy.getStatus().equals(PolicyStatus.PENDING_APPROVAL)) {
            throw new RuntimeException("Only PENDING_APPROVAL policies can be approved");
        }
        policy.setStatus(PolicyStatus.APPROVED);
        Policy savedPolicy = policyRepository.save(policy);
        outboxEventRepository.save(new OutboxEvent("policy-approved", policy.getId(), policy.getCreatedBy()));

        return savedPolicy;
    }

    @Transactional
    public Policy rejectPolicy(Long id) {
        Policy policy = getPolicyById(id);
        if (!policy.getStatus().equals(PolicyStatus.PENDING_APPROVAL)) {
            throw new RuntimeException("Only PENDING_APPROVAL policies can be rejected");
        }
        policy.setStatus(PolicyStatus.REJECTED);
        Policy savedPolicy = policyRepository.save(policy);
        outboxEventRepository.save(new OutboxEvent("policy-rejected", policy.getId(), policy.getCreatedBy()));

        return savedPolicy;
    }
}