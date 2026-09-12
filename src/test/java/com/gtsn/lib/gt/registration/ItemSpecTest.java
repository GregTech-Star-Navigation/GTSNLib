package com.gtsn.lib.gt.registration;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ItemSpec} 声明构建与校验的行为验证（#15）：不依赖任何 Minecraft / Forge / GTCEu 类型。
 */
class ItemSpecTest {

    @Test
    void buildsWithAllDeclarations() {
        ItemSpec spec = ItemSpec.builder("gtsnlib", "test_item")
                .maxStackSize(16)
                .fireResistant(true)
                .displayName("Test Item")
                .build();

        assertEquals("gtsnlib", spec.namespace());
        assertEquals("test_item", spec.id());
        assertEquals("gtsnlib:test_item", spec.key());
        assertEquals(16, spec.maxStackSize());
        assertTrue(spec.fireResistant());
        assertEquals(Optional.of("Test Item"), spec.displayName());
    }

    @Test
    void defaultsToStackOfSixtyFourAndNotFireResistant() {
        ItemSpec spec = ItemSpec.builder("gtsnlib", "test_item").build();

        assertEquals(ItemSpec.DEFAULT_MAX_STACK_SIZE, spec.maxStackSize());
        assertFalse(spec.fireResistant());
        assertTrue(spec.displayName().isEmpty());
        assertTrue(spec.hook().isEmpty());
    }

    @Test
    void normalizesNamespaceAndId() {
        ItemSpec spec = ItemSpec.builder(" GTSNLib ", " Test-Item ").build();

        assertEquals("gtsnlib", spec.namespace());
        assertEquals("test_item", spec.id());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> ItemSpec.builder("gtsnlib", "  ").build());
    }

    @Test
    void rejectsOutOfRangeMaxStackSize() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemSpec.builder("gtsnlib", "test_item").maxStackSize(0));
        assertThrows(IllegalArgumentException.class,
                () -> ItemSpec.builder("gtsnlib", "test_item").maxStackSize(65));
    }

    @Test
    void rejectsBlankDisplayName() {
        assertThrows(IllegalArgumentException.class,
                () -> ItemSpec.builder("gtsnlib", "test_item").displayName(" "));
    }

    @Test
    void storesHook() {
        RegistrationHook hook = (kind, id) -> {
        };

        ItemSpec spec = ItemSpec.builder("gtsnlib", "test_item").hook(hook).build();

        assertEquals(Optional.of(hook), spec.hook());
    }
}
