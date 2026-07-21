package com.dengene.api_gateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class FallbackController {

    // Called when the circuit breaker trips for governance-service — meaning
    // it's failed enough recent calls that we stop hammering it and respond
    // immediately instead of letting requests hang or time out.
    @GetMapping("/fallback/governance")
    public Mono<ResponseEntity<String>> governanceFallback() {
        return Mono.just(
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("{\"error\":\"Governance service is currently unavailable. Please try again shortly.\"}")
        );
    }

    @GetMapping("/fallback/audit")
    public Mono<ResponseEntity<String>> auditFallback() {
        return Mono.just(
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("{\"error\":\"Audit service is currently unavailable. Please try again shortly.\"}")
        );
    }
}