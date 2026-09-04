package com.techcombank.qe.sut;

import java.util.*;
import java.util.stream.*;

public final class CapabilityRegistry {

    /** All 24 archetypes, TST-020..TST-043 contiguous. Wave 15 closed every ID gap. */
    public static final Set<String> ALL = IntStream.rangeClosed(20, 43)
        .mapToObj(n -> "TST-0" + n)
        .collect(Collectors.toUnmodifiableSet());

    /** Implemented archetypes. Wave 16 added the first seven; each Wave 17 task
     *  adds exactly one ID here and to CapabilityRegistryTest's
     *  IMPLEMENTED_AT_WAVE_17 in the same commit -- see that test for the guard
     *  that keeps this set from drifting from what modules.yml actually ships. */
    public static final Set<String> IMPLEMENTED =
        Set.of("TST-021", "TST-023", "TST-026", "TST-027", "TST-028", "TST-029", "TST-030", "TST-031", "TST-034", "TST-035", "TST-037", "TST-039", "TST-040", "TST-043");

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
