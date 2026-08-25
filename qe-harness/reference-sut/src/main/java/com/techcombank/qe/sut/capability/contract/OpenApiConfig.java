package com.techcombank.qe.sut.capability.contract;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * TST-030 versioned API and breaking-change fixture (Task 10): publishes
 * the reference SUT's OpenAPI document at {@code GET /openapi.json} --
 * Task 20's Karate + Gatling module consumes it.
 *
 * <p>springdoc-openapi (added to {@code pom.xml} by this task) introspects
 * the running Spring MVC controllers -- including
 * {@link TransferV1Controller} and {@link TransferV2Controller} -- and
 * generates the document automatically; this class only supplies its
 * top-level {@code info} metadata. The document is served at
 * {@code /openapi.json} rather than springdoc's default {@code /v3/api-docs}
 * via {@code springdoc.api-docs.path} in
 * {@code src/main/resources/application.properties}.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI referenceSutOpenApi() {
        return new OpenAPI().info(new Info()
            .title("QE Harness Reference SUT")
            .version("1.0.0")
            .description("TST-030 versioned API capability: v1 and v2 /transfers, each "
                + "validated against its own published JSON Schema fixture in "
                + "src/main/resources/contracts/."));
    }
}
