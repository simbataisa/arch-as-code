package com.techcombank.qe.sut;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ReferenceSutApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReferenceSutApplication.class, args);
    }

    /**
     * Convenience default for ad-hoc local runs, e.g.
     * {@code SUT_DEFECT=ledger-unbalanced docker compose up}.
     *
     * <p>This is NOT the primary defect-injection mechanism — see
     * {@link DefectController} for that. It exists only so a developer can
     * boot the SUT already in a defective state without a follow-up curl.
     * An unrecognised value still fails startup: {@link DefectFlags#activate}
     * throws {@link IllegalArgumentException}, and an exception out of a
     * {@link CommandLineRunner} aborts {@code SpringApplication.run}.
     */
    @Bean
    CommandLineRunner activateDefectFromEnvironment() {
        return args -> {
            String flag = System.getenv("SUT_DEFECT");
            if (flag != null && !flag.isBlank()) {
                DefectFlags.activate(flag);
            }
        };
    }
}
