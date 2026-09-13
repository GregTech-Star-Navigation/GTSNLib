package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.render.SlotIcon;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 机器状态面板：从 MC-free {@link MachineStatusView} 装配能量条 / 进度箭头 / 槽位面板 / 状态文本，
 * 并做数据映射。
 *
 * <p>本套件不再需要 {@code MinecraftTestBootstrap}——面板层已无 MC 依赖（#22 评审 F1-3），
 * 测试直接用纯视图模型驱动。</p>
 */
class MachineStatusPanelTest {

    private static final SlotIcon ICON = (context, bounds) ->
            context.fill(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF00FF00);

    @Test
    void assemblesFromView() {
        MachineStatusView view = MachineStatusView.builder("gtsnlib:test_machine")
                .status("WORKING")
                .energy(500, 1000)
                .progress(40, 100)
                .working(true)
                .itemSlots(List.of(new MachineStatusView.Slot(0, null), new MachineStatusView.Slot(1, null)))
                .build();

        MachineStatusPanel panel = new MachineStatusPanel(view, PlainTextMetrics.INSTANCE);

        assertEquals(0.5, panel.energyBar().ratio(), 1e-9);
        assertEquals(0.4, panel.progressArrow().ratio(), 1e-9);
        assertTrue(panel.progressArrow().working());
        assertEquals(2, panel.slotsPanel().slotCount());
        assertEquals("gtsnlib:test_machine", panel.titleText().text());
        assertEquals("WORKING", panel.statusText().text());
    }

    @Test
    void handlesMachineWithoutEnergyOrSlots() {
        MachineStatusView view = MachineStatusView.builder("gtsnlib:test_machine").build();

        MachineStatusPanel panel = new MachineStatusPanel(view, PlainTextMetrics.INSTANCE);

        assertEquals(0.0, panel.energyBar().ratio(), 1e-9);
        assertFalse(panel.energyBar().hasEnergy());
        assertEquals(0, panel.slotsPanel().slotCount());
        assertTrue(panel.tanks().isEmpty());
    }

    @Test
    void mapsFluidTanksToTankWidgets() {
        MachineStatusView view = MachineStatusView.builder("gtsnlib:test_machine")
                .tanks(List.of(new MachineStatusView.Tank(0L, 1000L, "Water")))
                .build();

        MachineStatusPanel panel = new MachineStatusPanel(view, PlainTextMetrics.INSTANCE);

        assertEquals(1, panel.tanks().size());
        assertEquals(0L, panel.tanks().get(0).amount());
        assertEquals(1000L, panel.tanks().get(0).capacity());
        assertEquals("Water", panel.tanks().get(0).fluidName());
    }

    @Test
    void slotIndicesMapIconsIntoGrid() {
        MachineStatusView view = MachineStatusView.builder("gtsnlib:test_machine")
                .itemSlots(List.of(
                        new MachineStatusView.Slot(0, ICON),
                        new MachineStatusView.Slot(1, null),
                        new MachineStatusView.Slot(2, ICON)))
                .build();

        MachineStatusPanel panel = new MachineStatusPanel(view, PlainTextMetrics.INSTANCE);

        assertEquals(3, panel.slotsPanel().slotCount());
        assertSame(ICON, panel.slotsPanel().icon(0));
        assertEquals(null, panel.slotsPanel().icon(1));
        assertSame(ICON, panel.slotsPanel().icon(2));
    }
}
