package com.dengene.governance_service.service;

import com.dengene.audit_service.grpc.AuditLogReply;
import com.dengene.audit_service.grpc.AuditLogRequest;
import com.dengene.audit_service.grpc.AuditServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditGrpcClient {

    private final ManagedChannel auditGrpcChannel;

    public AuditLogReply confirmAuditLog(String eventType, Long policyId, String actor) {
        AuditServiceGrpc.AuditServiceBlockingStub stub = AuditServiceGrpc.newBlockingStub(auditGrpcChannel);

        AuditLogRequest request = AuditLogRequest.newBuilder()
                .setEventType(eventType)
                .setPolicyId(policyId)
                .setActor(actor)
                .build();

        return stub.createAuditLog(request);
    }
}