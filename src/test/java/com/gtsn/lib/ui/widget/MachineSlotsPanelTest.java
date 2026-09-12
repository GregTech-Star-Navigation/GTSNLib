package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.render.SlotIcon;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 机器槽位面板：图标列表到网格槽位的一一映射、列数/行数、空槽处理。
 */
class MachineSlotsPanelTest {

    private static final SlotIcon ICON_A = (context, bounds) ->
            context.fill(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF00FF00);
    private static final SlotIcon ICON_B = (context, bounds) ->
            context.fill(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFFFF0000);

    @Test
    void columnsMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new MachineSlotsPanel(List.of(), 0));
    }

    @Test
    void mapsIconsToSlotsInOrder() {
        MachineSlotsPanel panel = new MachineSlotsPanel(List.of(ICON_A, ICON_B, ICON_A), 2);

        assertEquals(3, panel.slotCount());
        assertEquals(2, panel.columns());
        assertEquals(2, panel.rows());
        assertSame(ICON_A, panel.icon(0));
        assertSame(ICON_B, panel.icon(1));
        assertSame(ICON_A, panel.icon(2));
        assertFalse(panel.slot(0).isEmpty());
        assertEquals(1, panel.children().size(), "网格作为单一子控件挂载");
    }

    @Test
    void nullIconsBecomeEmptySlots() {
        MachineSlotsPanel panel = new MachineSlotsPanel(Arrays.asList(ICON_A, null, ICON_B), 3);

        assertEquals(3, panel.slotCount());
        assertEquals(1, panel.rows());
        assertNull(panel.icon(1));
        assertTrue(panel.slot(1).isEmpty());
        assertFalse(panel.slot(2).isEmpty());
    }

    @Test
    void emptyListProducesNoSlots() {
        MachineSlotsPanel panel = new MachineSlotsPanel(List.of(), 4);

        assertEquals(0, panel.slotCount());
        assertEquals(0, panel.rows());
        assertThrows(IndexOutOfBoundsException.class, () -> panel.slot(0));
    }

    @Test
    void partialLastRowStillCountsAsRow() {
        MachineSlotsPanel panel = new MachineSlotsPanel(List.of(ICON_A, ICON_B, ICON_A, ICON_B, ICON_A), 2);

        assertEquals(5, panel.slotCount());
        assertEquals(3, panel.rows());
    }
}
