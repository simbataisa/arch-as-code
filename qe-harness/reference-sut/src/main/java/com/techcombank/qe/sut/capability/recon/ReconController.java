package com.techcombank.qe.sut.capability.recon;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TST-039 reconciliation capability's HTTP surface. Task 21's Locust module
 * scores {@code GET /recon/report} against the known-true set {@code POST
 * /recon/seed-defects} returns as a confusion matrix.
 */
@RestController
@RequestMapping("/recon")
public class ReconController {

    /** Fixed so every call seeds the exact same, reproducible defect set --
     *  "a known defect set" per the task brief, not a fresh random one each
     *  call. */
    private static final long SEED = 42L;

    private final ReconService reconService;
    private final DefectSeeder defectSeeder;

    public ReconController(ReconService reconService, DefectSeeder defectSeeder) {
        this.reconService = reconService;
        this.defectSeeder = defectSeeder;
    }

    /** GET /recon/report -> {"completeness": {...}, "accuracy": {...}, "timeliness": {...}} */
    @GetMapping("/report")
    public ReconReport report() {
        return reconService.report();
    }

    /** POST /recon/seed-defects -> 201 with the ground-truth {@link SeededDefects}
     *  this call just wrote, for the caller to score {@code GET /recon/report}
     *  against. */
    @PostMapping("/seed-defects")
    public ResponseEntity<SeededDefects> seedDefects() {
        SeededDefects seeded = defectSeeder.seed(SEED);
        return ResponseEntity.status(HttpStatus.CREATED).body(seeded);
    }
}
