package com.gtsn.lib.gt.registration;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MachineSpec} 声明构建与校验的行为验证（#15）：不依赖任何 Minecraft / Forge / GTCEu 类型。
 */
class MachineSpecTest {

    @Test
    void buildsWithAllDeclarations() {
        MachineSpec spec = MachineSpec.builder("gtsnlib", "test_machine")
                .tier(3)
                .displayName("Test Machine")
                .build();

        assertEquals("gtsnlib", spec.namespace());
        assertEquals("test_machine", spec.id());
        assertEquals("gtsnlib:test_machine", spec.key());
        assertEquals(3, spec.tier());
        assertEquals("Test Machine", spec.displayName());
    }

    @Test
    void defaultsToLowVoltageTierAndDerivesDisplayName() {
        MachineSpec spec = MachineSpec.builder("gtsnlib", "test_machine").build();

        assertEquals(MachineSpec.DEFAULT_TIER, spec.tier());
        assertEquals("Test Machine", spec.displayName());
    }

    @Test
    void normalizesNamespaceAndId() {
        MachineSpec spec = MachineSpec.builder(" GTSNLib ", " Test-Machine ").build();

        assertEquals("gtsnlib", spec.namespace());
        assertEquals("test_machine", spec.id());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> MachineSpec.builder("gtsnlib", "  ").build());
    }

    @Test
    void rejectsOutOfRangeTier() {
        assertThrows(IllegalArgumentException.class,
                () -> MachineSpec.builder("gtsnlib", "test_machine").tier(-1));
        assertThrows(IllegalArgumentException.class,
                () -> MachineSpec.builder("gtsnlib", "test_machine").tier(MachineSpec.MAX_TIER + 1));
    }

    @Test
    void rejectsBlankDisplayName() {
        assertThrows(IllegalArgumentException.class,
                () -> MachineSpec.builder("gtsnlib", "test_machine").displayName(" "));
    }

    @Test
    void storesHook() {
        RegistrationHook hook = (kind, id) -> {
        };

        MachineSpec spec = MachineSpec.builder("gtsnlib", "test_machine").hook(hook).build();

        assertTrue(spec.hook().isPresent());
        assertEquals(hook, spec.hook().orElseThrow());
    }
}
