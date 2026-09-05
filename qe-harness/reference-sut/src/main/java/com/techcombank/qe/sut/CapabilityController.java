package com.techcombank.qe.sut;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Meta/capability-discovery endpoints for the reference SUT.
 *
 * <p>{@code _}-prefixed paths (here and in {@link DefectController}) mark
 * endpoints that exist only because this SUT's entire purpose is to be a
 * deliberately-defect-toggleable reference service — a real production
 * service would never expose them.
 */
@RestController
public class CapabilityController {

    /** GET /_capabilities -> {"TST-020": "implemented", "TST-022": "declared", ...} */
    @GetMapping("/_capabilities")
    public Map<String, String> capabilities() {
        return CapabilityRegistry.statusMap();
    }

    /**
     * GET /capability/{archetype}/probe
     *
     * <p>Returns 501 Not Implemented with {@code {"archetype", "status": "declared",
     * "wave": "17+"}} for any archetype not yet in {@link CapabilityRegistry#IMPLEMENTED}.
     * Returns 400 Bad Request for an archetype outside the known 24. Implemented
     * archetypes (Tasks 6-13 onward) get a 200 acknowledging their status; this
     * task ships zero of those, since {@code IMPLEMENTED} starts as {@code Set.of()}.
     */
    @GetMapping("/capability/{archetype}/probe")
    public ResponseEntity<Map<String, String>> probe(@PathVariable String archetype) {
        String status;
        try {
            status = CapabilityRegistry.statusOf(archetype);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        if ("implemented".equals(status)) {
            return ResponseEntity.ok(Map.of("archetype", archetype, "status", status));
        }

        Map<String, String> body = new LinkedHashMap<>();
        body.put("archetype", archetype);
        body.put("status", "declared");
        body.put("wave", "17+");
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(body);
    }
}
