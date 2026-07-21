package com.dengene.governance_service.config;

import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    // @LoadBalanced makes this RestTemplate Eureka-aware — calling
    // "http://audit-service/..." resolves through service discovery instead
    // of needing a hardcoded host:port.
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}