package com.gtsn.lib.api;

import com.gtsn.lib.testing.MinecraftTestBootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * GT 机器只读快照模型：默认值、比例钳制、负值归一、列表不可变与流体容量回退。
 *
 * <p>{@code ItemStack}/{@code FluidStack} 的静态初始化需要 Minecraft 注册表已引导，
 * 因此先经 {@link MinecraftTestBootstrap} 做无游戏环境引导。</p>
 */
class GtMachineSnapshotTest {

    @BeforeAll
    static void bootstrapRegistries() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    void machineIdIsRequiredAndBlankRejected() {
        assertThrows(NullPointerException.class, () -> GtMachineSnapshot.builder(null));
        assertThrows(IllegalArgumentException.class, () -> GtMachineSnapshot.builder("  "));
    }

    @Test
    void defaultsAreEmptyAndUnknown() {
        GtMachineSnapshot snapshot = GtMachineSnapshot.builder("gtsnlib:test_machine").build();

        assertEquals("gtsnlib:test_machine", snapshot.machineId());
        assertEquals(0, snapshot.tier());
        assertEquals(GtMachineSnapshot.STATUS_UNKNOWN, snapshot.status());
        assertEquals(0L, snapshot.energyStored());
        assertEquals(0L, snapshot.energyCapacity());
        assertEquals(0L, snapshot.inputVoltage());
        assertEquals(0, snapshot.progress());
        assertEquals(0, snapshot.maxProgress());
        assertFalse(snapshot.working());
        assertTrue(snapshot.itemSlots().isEmpty());
        assertTrue(snapshot.fluidTanks().isEmpty());
        assertFalse(snapshot.hasEnergy());
        assertFalse(snapshot.hasRecipeProgress());
    }

    @Test
    void energyRatioClampsToUnitInterval() {
        assertEquals(0.2, GtMachineSnapshot.builder("m").energy(200, 1000).build().energyRatio(), 1e-9);
        assertEquals(1.0, GtMachineSnapshot.builder("m").energy(2000, 1000).build().energyRatio(), 1e-9);
        assertEquals(0.0, GtMachineSnapshot.builder("m").energy(0, 0).build().energyRatio(), 1e-9);
        assertEquals(0.0, GtMachineSnapshot.builder("m").energy(50, -100).build().energyRatio(), 1e-9);
    }

    @Test
    void negativeReadingsNormalizeToZero() {
        GtMachineSnapshot snapshot = GtMachineSnapshot.builder("m")
                .energy(-200, -1000)
                .progress(-5, -10)
                .build();

        assertEquals(0L, snapshot.energyStored());
        assertEquals(0L, snapshot.energyCapacity());
        assertEquals(0, snapshot.progress());
        assertEquals(0, snapshot.maxProgress());
        assertFalse(snapshot.hasEnergy());
        assertFalse(snapshot.hasRecipeProgress());
    }

    @Test
    void progressRatioClampsToUnitInterval() {
        assertEquals(0.5, GtMachineSnapshot.builder("m").progress(50, 100).build().progressRatio(), 1e-9);
        assertEquals(1.0, GtMachineSnapshot.builder("m").progress(150, 100).build().progressRatio(), 1e-9);
        assertEquals(0.0, GtMachineSnapshot.builder("m").progress(40, 0).build().progressRatio(), 1e-9);
        assertTrue(GtMachineSnapshot.builder("m").progress(40, 80).build().hasRecipeProgress());
    }

    @Test
    void listsAreDefensiveUnmodifiableCopies() {
        List<ItemStack> slots = new ArrayList<>();
        slots.add(ItemStack.EMPTY);
        List<FluidStack> tanks = new ArrayList<>();
        tanks.add(FluidStack.EMPTY);
        List<Long> capacities = new ArrayList<>();
        capacities.add(8000L);

        GtMachineSnapshot snapshot = GtMachineSnapshot.builder("m")
                .itemSlots(slots)
                .fluidTanks(tanks)
                .fluidTankCapacities(capacities)
                .build();

        slots.add(ItemStack.EMPTY);
        tanks.add(FluidStack.EMPTY);
        capacities.add(16000L);

        assertEquals(1, snapshot.itemSlots().size());
        assertEquals(1, snapshot.fluidTanks().size());
        assertEquals(1, snapshot.fluidTankCapacities().size());
        assertThrows(UnsupportedOperationException.class, () -> snapshot.itemSlots().add(ItemStack.EMPTY));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.fluidTanks().add(FluidStack.EMPTY));
    }

    @Test
    void nullListElementsAreRejected() {
        List<FluidStack> withNull = new ArrayList<>();
        withNull.add(null);
        assertThrows(NullPointerException.class,
                () -> GtMachineSnapshot.builder("m").fluidTanks(withNull));

        List<ItemStack> slotsWithNull = new ArrayList<>();
        slotsWithNull.add(null);
        assertThrows(NullPointerException.class,
                () -> GtMachineSnapshot.builder("m").itemSlots(slotsWithNull));
    }

    @Test
    void fluidCapacityFallsBackToZeroWhenMissing() {
        GtMachineSnapshot withCapacity = GtMachineSnapshot.builder("m")
                .fluidTankCapacities(List.of(8000L))
                .build();
        assertEquals(8000L, withCapacity.fluidCapacity(0));
        assertEquals(0L, withCapacity.fluidCapacity(3));

        GtMachineSnapshot withoutCapacity = GtMachineSnapshot.builder("m").build();
        assertEquals(0L, withoutCapacity.fluidCapacity(0));
        assertEquals(0L, withoutCapacity.fluidCapacity(-1));
    }
}
