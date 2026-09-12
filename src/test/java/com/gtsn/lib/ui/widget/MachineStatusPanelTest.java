package com.gtsn.lib.ui.widget;

import com.gtsn.lib.api.GtMachineSnapshot;
import com.gtsn.lib.testing.MinecraftTestBootstrap;
import com.gtsn.lib.ui.render.SlotIcon;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 机器状态面板：从快照装配能量条 / 进度箭头 / 槽位面板 / 状态文本，并做数据映射。
 */
class MachineStatusPanelTest {

    @BeforeAll
    static void bootstrapRegistries() {
        MinecraftTestBootstrap.ensure();
    }

    private static final SlotIcon ICON = (context, bounds) ->
            context.fill(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF00FF00);
    private static final Function<ItemStack, SlotIcon> MAPPER = stack -> ICON;

    @Test
    void assemblesFromSnapshot() {
        GtMachineSnapshot snapshot = GtMachineSnapshot.builder("gtsnlib:test_machine")
                .tier(1)
                .status("WORKING")
                .energy(500, 1000)
                .inputVoltage(32)
                .progress(40, 100)
                .working(true)
                .itemSlots(List.of(ItemStack.EMPTY, ItemStack.EMPTY))
                .build();

        MachineStatusPanel panel = new MachineStatusPanel(snapshot, PlainTextMetrics.INSTANCE, MAPPER);

        assertEquals(0.5, panel.energyBar().ratio(), 1e-9);
        assertEquals(0.4, panel.progressArrow().ratio(), 1e-9);
        assertTrue(panel.progressArrow().working());
        assertEquals(2, panel.slotsPanel().slotCount());
        assertEquals("gtsnlib:test_machine", panel.titleText().text());
        assertEquals("WORKING", panel.statusText().text());
    }

    @Test
    void handlesMachineWithoutEnergyOrSlots() {
        GtMachineSnapshot snapshot = GtMachineSnapshot.builder("gtsnlib:test_machine").tier(1).build();

        MachineStatusPanel panel = new MachineStatusPanel(snapshot, PlainTextMetrics.INSTANCE, MAPPER);

        assertEquals(0.0, panel.energyBar().ratio(), 1e-9);
        assertFalse(panel.energyBar().hasEnergy());
        assertEquals(0, panel.slotsPanel().slotCount());
        assertTrue(panel.tanks().isEmpty());
    }

    @Test
    void mapsFluidTanksToTankWidgets() {
        GtMachineSnapshot snapshot = GtMachineSnapshot.builder("gtsnlib:test_machine")
                .fluidTanks(List.of(FluidStack.EMPTY))
                .fluidTankCapacities(List.of(1000L))
                .build();

        MachineStatusPanel panel = new MachineStatusPanel(snapshot, PlainTextMetrics.INSTANCE, MAPPER);

        assertEquals(1, panel.tanks().size());
        assertEquals(0L, panel.tanks().get(0).amount());
        assertEquals(1000L, panel.tanks().get(0).capacity());
    }
}
