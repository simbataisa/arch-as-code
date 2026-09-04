package com.techcombank.qe.sut;

import com.techcombank.qe.sut.data.SeedSummary;
import com.techcombank.qe.sut.data.SyntheticDataSeeder;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Meta/test-control endpoint for seeding synthetic data over HTTP (Wave 17).
 *
 * <p>{@code _test}-prefixed, matching {@link DefectController} and
 * {@code RateLimitResetController}. Harness modules run as separate tool
 * processes against an already-running container, so they can only reach
 * {@link SyntheticDataSeeder} -- which had no HTTP trigger at all before this
 * -- over HTTP. TST-034's blended run needs a wider account set than the
 * two-account ledger fixture, and needs to reseed between runs without a
 * container restart, exactly as TST-031 needed
 * {@code POST /_test/reset/ratelimit}.
 *
 * <p>{@code @Profile("!prod")} -- see {@link DefectController}'s javadoc.
 */
@RestController
@Profile("!prod")
public class TestSeedController {

    private final SyntheticDataSeeder seeder;

    public TestSeedController(SyntheticDataSeeder seeder) {
        this.seeder = seeder;
    }

    /** POST /_test/seed?seed=42&accounts=20 -> 201 {accounts, entries}.
     *  Both parameters are explicit so a run's fixture is reproducible from its
     *  own request line; the seed defaults to the same fixed 42 ReconController
     *  uses, for the same reason -- a known set, not a fresh random one. */
    @PostMapping("/_test/seed")
    public ResponseEntity<?> seed(@RequestParam(defaultValue = "42") long seed,
                                  @RequestParam(defaultValue = "20") int accounts) {
        try {
            SeedSummary summary = seeder.seed(seed, accounts);
            return ResponseEntity.status(HttpStatus.CREATED).body(summary);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
