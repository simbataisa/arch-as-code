package com.techcombank.qe.sut;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CapabilityRegistryTest {

    /** The registry's expected contents. Each Wave 17 task that implements an
     *  archetype appends its ID here in the same commit that adds it to
     *  CapabilityRegistry.IMPLEMENTED -- so this set is the drift guard, and the
     *  suite never runs knowingly red. Wave 16 left seven; Wave 17 adds eight. */
    private static final Set<String> IMPLEMENTED_AT_WAVE_17 = Set.of(
        "TST-020", "TST-021", "TST-023", "TST-026", "TST-027", "TST-028", "TST-029", "TST-030", "TST-031", "TST-034", "TST-035", "TST-037", "TST-039", "TST-040", "TST-043"
    );

    @Test
    void enumeratesAllTwentyFourArchetypes() {
        assertEquals(24, CapabilityRegistry.ALL.size());
        assertTrue(CapabilityRegistry.ALL.contains("TST-020"));
        assertTrue(CapabilityRegistry.ALL.contains("TST-043"));
    }

    @Test
    void archetypeIdsAreContiguousFrom020To043() {
        for (int n = 20; n <= 43; n++) {
            assertTrue(CapabilityRegistry.ALL.contains(String.format("TST-0%d", n)),
                       "missing TST-0" + n);
        }
    }

    @Test
    void implementedIsASubsetOfAll() {
        assertTrue(CapabilityRegistry.ALL.containsAll(CapabilityRegistry.IMPLEMENTED));
    }

    @Test
    void implementedMatchesTheDeclaredSetExactly() {
        assertEquals(IMPLEMENTED_AT_WAVE_17, CapabilityRegistry.IMPLEMENTED);
    }

    @Test
    void declaredAndImplementedPartitionAllTwentyFour() {
        long declared = CapabilityRegistry.ALL.stream()
            .filter(a -> "declared".equals(CapabilityRegistry.statusOf(a))).count();
        assertEquals(CapabilityRegistry.ALL.size() - IMPLEMENTED_AT_WAVE_17.size(), declared);
    }

    @Test
    void statusOfAnUnimplementedArchetypeIsDeclared() {
        assertFalse(IMPLEMENTED_AT_WAVE_17.contains("TST-022"),
                    "TST-022 is out of Wave 17's scope and must stay declared");
        assertEquals("declared", CapabilityRegistry.statusOf("TST-022"));
    }
}
