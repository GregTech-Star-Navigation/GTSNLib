package com.gtsn.lib.ui.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 数据同步演示的服务端推进规则单测（纯函数）。
 */
class DemoSyncTest {

    @Test
    void advanceProgressStepsAndWrapsAtMax() {
        assertEquals(5, DemoSync.advanceProgress(0, DemoSync.PROGRESS_STEP));
        assertEquals(95, DemoSync.advanceProgress(90, DemoSync.PROGRESS_STEP));
        assertEquals(100, DemoSync.advanceProgress(95, DemoSync.PROGRESS_STEP));
        assertEquals(0, DemoSync.advanceProgress(100, DemoSync.PROGRESS_STEP));
    }

    @Test
    void phaseReflectsProgressBoundaries() {
        assertEquals(DemoPhase.IDLE, DemoSync.phaseFor(0));
        assertEquals(DemoPhase.RUNNING, DemoSync.phaseFor(50));
        assertEquals(DemoPhase.DONE, DemoSync.phaseFor(100));
    }

    @Test
    void tierCyclesWithinCount() {
        assertEquals(1, DemoSync.nextTier(0));
        assertEquals(2, DemoSync.nextTier(1));
        assertEquals(0, DemoSync.nextTier(DemoSync.TIER_COUNT - 1));
    }

    @Test
    void speedFollowsTierWithinDeclaredRange() {
        assertEquals(1f, DemoSync.speedForTier(0), 0f);
        assertEquals(5f, DemoSync.speedForTier(4), 0f);
    }

    @Test
    void layoutCoversAllDemoSlots() {
        assertEquals(5, DemoSync.LAYOUT.slotCount());
        assertEquals("progress", DemoSync.LAYOUT.slotAt(0).name());
        assertEquals("speed", DemoSync.LAYOUT.slotAt(1).name());
        assertEquals("active", DemoSync.LAYOUT.slotAt(2).name());
        assertEquals("phase", DemoSync.LAYOUT.slotAt(3).name());
        assertEquals("tier", DemoSync.LAYOUT.slotAt(4).name());
    }
}
