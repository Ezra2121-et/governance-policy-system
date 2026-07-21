package com.dengene.governance_service.config;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcClientConfig {

    // Points directly at audit-service's gRPC port. In a fuller setup this
    // would resolve via Eureka too, but a static channel keeps this focused
    // on demonstrating the gRPC call itself for the comparison writeup.
    @Bean
    public ManagedChannel auditGrpcChannel() {
        return ManagedChannelBuilder.forAddress("localhost", 9091)
                .usePlaintext()
                .build();
    }
}