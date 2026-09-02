package com.techcombank.qe.sut.capability.clientexp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * TST-043 client-experience capability's HTTP surface. Task 22's k6 module
 * drives this single endpoint; all caching, conditional-request, and
 * compression behaviour lives upstream in {@link CachePolicyFilter}, so this
 * controller body is a plain, deterministic payload.
 *
 * <p>The response body is intentionally static across requests: {@link
 * CachePolicyFilter}'s strong ETag is a hash of the body, and a stable body
 * is what makes the ETag stable request-to-request -- the property
 * {@code matchingIfNoneMatchYieldsNotModifiedWithNoBody} in {@code
 * CachePolicyTest} depends on.
 */
@RestController
public class CatalogueController {

    /** GET /catalogue -> 200 with a small, fixed catalogue payload.
     *  {@link CachePolicyFilter} adds Cache-Control/ETag, handles
     *  conditional requests, and compresses the body on request. */
    @GetMapping("/catalogue")
    public List<Map<String, Object>> catalogue() {
        return List.of(
            Map.of("sku", "CTLG-001", "name", "Reference Widget", "priceCents", 1999),
            Map.of("sku", "CTLG-002", "name", "Reference Gadget", "priceCents", 2999),
            Map.of("sku", "CTLG-003", "name", "Reference Gizmo", "priceCents", 3999)
        );
    }
}
