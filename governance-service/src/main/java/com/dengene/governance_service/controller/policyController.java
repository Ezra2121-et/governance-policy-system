package com.dengene.governance_service.controller;
import com.dengene.governance_service.dto.policyCreateRequest;
import com.dengene.governance_service.dto.PolicyResponse;
import com.dengene.governance_service.model.Policy;
import com.dengene.governance_service.service.PolicyApprovalSagaService;
import com.dengene.governance_service.service.PolicyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/policies")
@RequiredArgsConstructor
public class policyController {

    private final PolicyService policyService;
    private final PolicyApprovalSagaService policyApprovalSagaService;

    @Value("${server.port}")
    private String port;

    @PostMapping
    public ResponseEntity<PolicyResponse> createPolicy(@RequestBody policyCreateRequest request) {
        Policy policy = policyService.createPolicy(request.getTitle(), request.getDescription(), request.getCreatedBy());
        return ResponseEntity.status(HttpStatus.CREATED).body(PolicyResponse.fromPolicy(policy));
    }

    @GetMapping
    public ResponseEntity<List<PolicyResponse>> getAllPolicies() {
        System.out.println(">>> Handled by instance on port: " + port);
        List<Policy> policies = policyService.getAllPolicies();
        List<PolicyResponse> responses = policies.stream()
                .map(PolicyResponse::fromPolicy)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PolicyResponse> getPolicyById(@PathVariable Long id) {
        Policy policy = policyService.getPolicyById(id);
        return ResponseEntity.ok(PolicyResponse.fromPolicy(policy));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<PolicyResponse> submitPolicy(@PathVariable Long id) {
        Policy policy = policyService.submitPolicy(id);
        return ResponseEntity.ok(PolicyResponse.fromPolicy(policy));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<PolicyResponse> approvePolicy(@PathVariable Long id) {
        Policy policy = policyApprovalSagaService.approveWithSaga(id);
        return ResponseEntity.ok(PolicyResponse.fromPolicy(policy));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<PolicyResponse> rejectPolicy(@PathVariable Long id) {
        Policy policy = policyService.rejectPolicy(id);
        return ResponseEntity.ok(PolicyResponse.fromPolicy(policy));
    }
}