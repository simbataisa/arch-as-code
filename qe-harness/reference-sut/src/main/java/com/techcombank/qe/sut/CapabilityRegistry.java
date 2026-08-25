package com.techcombank.qe.sut;

import java.util.*;
import java.util.stream.*;

public final class CapabilityRegistry {

    /** All 24 archetypes, TST-020..TST-043 contiguous. Wave 15 closed every ID gap. */
    public static final Set<String> ALL = IntStream.rangeClosed(20, 43)
        .mapToObj(n -> "TST-0" + n)
        .collect(Collectors.toUnmodifiableSet());

    /** Implemented in Wave 16. Tasks 6-13 each add exactly one ID here. */
    public static final Set<String> IMPLEMENTED =
        Set.of("TST-021", "TST-030", "TST-031", "TST-035", "TST-039", "TST-040");

    private CapabilityRegistry() {}

    public static String statusOf(String archetype) {
        if (!ALL.contains(archetype)) {
            throw new IllegalArgumentException("unknown archetype: " + archetype);
        }
        return IMPLEMENTED.contains(archetype) ? "implemented" : "declared";
    }

    public static Map<String, String> statusMap() {
        return ALL.stream().sorted()
            .collect(Collectors.toMap(a -> a, CapabilityRegistry::statusOf,
                                      (a, b) -> a, LinkedHashMap::new));
    }
}
