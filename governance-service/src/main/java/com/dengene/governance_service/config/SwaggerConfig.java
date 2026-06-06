package com.dengene.governance_service.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI governanceServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Governance Service API")
                        .description("API for managing governance policies and their lifecycle")
                        .version("1.0.0"));
    }
}