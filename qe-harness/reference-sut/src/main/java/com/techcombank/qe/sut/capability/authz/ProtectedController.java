package com.techcombank.qe.sut.capability.authz;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * TST-040's three role-gated resources. Access control itself lives entirely
 * in {@link SecurityConfig}; every method here only ever runs once
 * {@code SecurityConfig} has already let the request through, so a 200 from
 * any of these three is exactly what {@link AuthzDecisionFilter} marks
 * {@code allow}.
 *
 * <p>All three are {@code GET} uniformly, including "write" and "admin" --
 * this reference SUT's authorization matrix varies by role and endpoint, not
 * by HTTP method; Task 19's JMeter module is what exercises real
 * method/operation combinations against the full archetype.
 */
@RestController
@RequestMapping("/protected")
public class ProtectedController {

    @GetMapping("/read")
    public Map<String, String> read() {
        return Map.of("resource", "read");
    }

    @GetMapping("/write")
    public Map<String, String> write() {
        return Map.of("resource", "write");
    }

    @GetMapping("/admin")
    public Map<String, String> admin() {
        return Map.of("resource", "admin");
    }
}
