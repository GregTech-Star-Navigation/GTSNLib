package com.gtsn.lib.ui.sync;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 槽位注册表（{@link SyncLayout}）单测：索引分配、名称唯一、冻结语义与默认值编码。
 */
class SyncLayoutTest {

    @Test
    void builderAssignsSequentialIndicesInDeclarationOrder() {
        SyncLayout.Builder builder = SyncLayout.builder();
        SyncSlot<Integer> progress = builder.slot("progress", SyncCodecs.intRange(0, 100), 0);
        SyncSlot<Float> speed = builder.slot("speed", SyncCodecs.floatRange(0f, 5f), 1f);
        SyncSlot<Boolean> active = builder.slot("active", SyncCodecs.bools(), true);
        SyncLayout layout = builder.build();

        assertEquals(0, progress.index());
        assertEquals(1, speed.index());
        assertEquals(2, active.index());
        assertEquals(3, layout.slotCount());
    }

    @Test
    void builderRejectsDuplicateNames() {
        SyncLayout.Builder builder = SyncLayout.builder();
        builder.slot("value", SyncCodecs.ints(), 0);
        assertThrows(IllegalArgumentException.class, () -> builder.slot("value", SyncCodecs.ints(), 1));
    }

    @Test
    void builderRejectsNullArguments() {
        SyncLayout.Builder builder = SyncLayout.builder();
        assertThrows(NullPointerException.class, () -> builder.slot(null, SyncCodecs.ints(), 0));
        assertThrows(NullPointerException.class, () -> builder.slot("x", null, 0));
        assertThrows(NullPointerException.class, () -> builder.slot("x", SyncCodecs.ints(), null));
    }

    @Test
    void layoutExposesSlotsByIndexAndName() {
        SyncLayout.Builder builder = SyncLayout.builder();
        SyncSlot<Integer> first = builder.slot("first", SyncCodecs.ints(), 1);
        SyncSlot<Integer> second = builder.slot("second", SyncCodecs.ints(), 2);
        SyncLayout layout = builder.build();

        assertSame(first, layout.slotAt(0));
        assertSame(second, layout.slotAt(1));
        assertSame(second, layout.slot("second"));
        assertEquals(List.of(first, second), layout.slots());
        assertThrows(IndexOutOfBoundsException.class, () -> layout.slotAt(2));
        assertThrows(IllegalArgumentException.class, () -> layout.slot("missing"));
    }

    @Test
    void layoutIsFrozenAfterBuild() {
        SyncLayout.Builder builder = SyncLayout.builder();
        builder.slot("value", SyncCodecs.ints(), 0);
        SyncLayout layout = builder.build();

        assertThrows(IllegalStateException.class, () -> builder.slot("late", SyncCodecs.ints(), 1));
        assertThrows(UnsupportedOperationException.class, () -> layout.slots().add(null));
    }

    @Test
    void slotDefaultValueIsEncodedToRawWithCodecRules() {
        SyncLayout.Builder builder = SyncLayout.builder();
        SyncSlot<Integer> clamped = builder.slot("clamped", SyncCodecs.intRange(0, 10), 42);
        SyncSlot<Float> speed = builder.slot("speed", SyncCodecs.floats(), 1.5f);
        SyncLayout layout = builder.build();

        assertEquals(10, clamped.defaultRaw());
        assertEquals(10, clamped.defaultValue());
        assertEquals(1.5f, Float.intBitsToFloat(speed.defaultRaw()), 0f);
        assertEquals(2, layout.slotCount());
        assertTrue(layout.slots().contains(clamped));
    }
}
