package com.dengene.audit_service.grpc;

import com.dengene.audit_service.model.AuditLog;
import com.dengene.audit_service.service.AuditLogService;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDateTime;

// The gRPC counterpart to AuditLogController's REST /audit-logs/confirm
// endpoint — same underlying write, different transport/protocol. Useful
// for directly comparing REST vs gRPC on the same operation.
@GrpcService
@RequiredArgsConstructor
public class AuditGrpcServer extends AuditServiceGrpc.AuditServiceImplBase {

    private final AuditLogService auditLogService;

    @Override
    public void createAuditLog(AuditLogRequest request, StreamObserver<AuditLogReply> responseObserver) {
        AuditLog savedLog = auditLogService.logEvent(
                request.getEventType(),
                request.getPolicyId(),
                request.getActor(),
                LocalDateTime.now()
        );

        AuditLogReply reply = AuditLogReply.newBuilder()
                .setId(savedLog.getId())
                .setStatus("CREATED")
                .build();

        responseObserver.onNext(reply);
        responseObserver.onCompleted();
    }
}