package com.techcombank.qe.sut;

import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;

class CapabilityRegistryTest {

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
    void statusOfDeclaredButUnimplementedIsDeclared() {
        assertEquals("declared", CapabilityRegistry.statusOf("TST-022"));
    }

    @Test
    void waveSixteenImplementsExactlySevenCapabilities() {
        assertEquals(7, CapabilityRegistry.IMPLEMENTED.size());
        assertEquals(Set.of("TST-021","TST-030","TST-031","TST-035","TST-039","TST-040","TST-043"),
                     CapabilityRegistry.IMPLEMENTED);
    }

    @Test
    void seventeenArchetypesRemainDeclared() {
        long declared = CapabilityRegistry.ALL.stream()
            .filter(a -> "declared".equals(CapabilityRegistry.statusOf(a))).count();
        assertEquals(17, declared);
    }
}
