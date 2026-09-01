package com.techcombank.qe.sut;

import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Meta/test-control endpoints for toggling {@link DefectFlags} at runtime.
 *
 * <p>These are the primary defect-injection mechanism the harness (Tasks
 * 16-23) actually uses: the harness modules run as separate tool processes
 * (JMeter/Karate/Locust/k6, or shell scripts) against an already-running
 * {@code docker compose up} container, so they toggle defects over HTTP
 * rather than via an environment variable on the container.
 *
 * <p>{@code _}-prefixed, matching the {@code /_capabilities} convention —
 * this exists only because this SUT's purpose is to be
 * deliberately-defect-toggleable, not because a real production service
 * would expose it.
 *
 * <p>{@code @Profile("!prod")}: this controller is unauthenticated by
 * {@link com.techcombank.qe.sut.capability.authz.SecurityConfig} design
 * ({@code permitAll} outside {@code /protected/**}), so it is registered in
 * every profile except one explicitly named {@code prod} — a no-op today
 * (no profile named {@code prod} is ever active anywhere this harness runs;
 * see {@code docker-compose.yml}, which has no {@code SPRING_PROFILES_ACTIVE}),
 * and a real guard the moment a team copying this reference implementation
 * deploys it with {@code prod} active. See the "Copying this reference
 * implementation" section of {@code qe-harness/README.md}.
 */
@RestController
@RequestMapping("/_test/defect")
@Profile("!prod")
public class DefectController {

    /** POST /_test/defect/{flag} -> 204, or 400 with the flag name on an unknown flag. */
    @PostMapping("/{flag}")
    public ResponseEntity<String> activate(@PathVariable String flag) {
        try {
            DefectFlags.activate(flag);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(flag);
        }
        return ResponseEntity.noContent().build();
    }

    /** DELETE /_test/defect -> 204, always clears whatever (if anything) was active. */
    @DeleteMapping
    public ResponseEntity<Void> clear() {
        DefectFlags.clear();
        return ResponseEntity.noContent().build();
    }
}
