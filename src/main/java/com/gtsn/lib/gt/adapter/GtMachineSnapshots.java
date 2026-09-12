package com.gtsn.lib.gt.adapter;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import com.gregtechceu.gtceu.api.machine.feature.IRecipeLogicMachine;
import com.gregtechceu.gtceu.api.machine.trait.RecipeLogic;
import com.gtsn.lib.api.GtMachineSnapshot;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 从 GTCEu 机器构建 {@link GtMachineSnapshot} 的适配器（ADR-0005：唯一允许接触 GTCEu 类型的边界）。
 *
 * <p>两条入口：</p>
 * <ul>
 *   <li>{@link #of(MetaMachine)} —— 已有的服务端 / 客户端镜像机器实例；</li>
 *   <li>{@link #at(Level, BlockPos)} —— 按坐标解析方块实体并读取其镜像机器；客户端读的是 LDLib 托管
 *       同步（{@code @DescSynced}）后的值，因此可逐帧 / 逐 tick 安全刷新。</li>
 * </ul>
 *
 * <p><b>只读边界</b>：本类只调用读取方法（{@code getEnergyStored} / {@code getRecipeLogic} /
 * {@code getStackInSlot} 等），从不修改机器状态，也不打开 GT 的 LDLib 界面。返回的
 * {@link GtMachineSnapshot} 只含 GTSN 自有类型与 MC 公共类型（{@code ItemStack}/{@code FluidStack}），
 * 不含 GTCEu 类型。</p>
 *
 * <p>能力读取统一经 {@link GTCapabilityHelper}（可空安全），缺失能力的机器返回 0 / 空列表而不是失败。</p>
 */
public final class GtMachineSnapshots {

    private GtMachineSnapshots() {
    }

    /** 目标坐标是否是一台 GT 机器（用于开发入口判定）。 */
    public static boolean isGtMachine(Level level, BlockPos pos) {
        return resolve(level, pos) != null;
    }

    /**
     * 按坐标读取机器并构建快照；坐标处无 GT 机器时返回空。
     *
     * <p>服务端读到权威值；客户端读到的是方块实体上镜像机器的同步值（LDLib 托管同步）。</p>
     */
    public static Optional<GtMachineSnapshot> at(Level level, BlockPos pos) {
        MetaMachine machine = resolve(level, pos);
        if (machine == null) {
            return Optional.empty();
        }
        return Optional.of(of(machine));
    }

    /** 从已有机器实例构建快照。 */
    public static GtMachineSnapshot of(MetaMachine machine) {
        if (machine == null) {
            throw new NullPointerException("machine");
        }
        Level level = machine.getLevel();
        BlockPos pos = machine.getPos();
        MachineDefinition definition = machine.getDefinition();

        String machineId = definition != null ? definition.getId().toString() : "gtsnlib:unknown";
        int tier = definition != null ? definition.getTier() : 0;

        long energyStored = 0L;
        long energyCapacity = 0L;
        long inputVoltage = 0L;
        IEnergyContainer energy = level != null && pos != null
                ? GTCapabilityHelper.getEnergyContainer(level, pos, null)
                : null;
        if (energy != null) {
            energyStored = energy.getEnergyStored();
            energyCapacity = energy.getEnergyCapacity();
            inputVoltage = energy.getInputVoltage();
        }

        int progress = 0;
        int maxProgress = 0;
        boolean working = false;
        String status = GtMachineSnapshot.STATUS_UNKNOWN;
        RecipeLogic recipeLogic = resolveRecipeLogic(machine, level, pos);
        if (recipeLogic != null) {
            progress = recipeLogic.getProgress();
            maxProgress = recipeLogic.getMaxProgress();
            working = recipeLogic.isWorking();
            if (recipeLogic.getStatus() != null) {
                status = recipeLogic.getStatus().name();
            }
        }

        IItemHandler items = level != null && pos != null
                ? GTCapabilityHelper.getItemHandler(level, pos, null)
                : null;
        List<ItemStack> itemSlots = new ArrayList<>();
        if (items != null) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                ItemStack stack = items.getStackInSlot(slot);
                itemSlots.add(stack == null ? ItemStack.EMPTY : stack.copy());
            }
        }

        IFluidHandler fluids = level != null && pos != null
                ? GTCapabilityHelper.getFluidHandler(level, pos, null)
                : null;
        List<FluidStack> fluidTanks = new ArrayList<>();
        List<Long> fluidCapacities = new ArrayList<>();
        if (fluids != null) {
            for (int tank = 0; tank < fluids.getTanks(); tank++) {
                FluidStack fluid = fluids.getFluidInTank(tank);
                fluidTanks.add(fluid == null ? FluidStack.EMPTY : fluid.copy());
                fluidCapacities.add((long) fluids.getTankCapacity(tank));
            }
        }

        return GtMachineSnapshot.builder(machineId)
                .tier(tier)
                .status(status)
                .energy(energyStored, energyCapacity)
                .inputVoltage(inputVoltage)
                .progress(progress, maxProgress)
                .working(working)
                .itemSlots(itemSlots)
                .fluidTanks(fluidTanks)
                .fluidTankCapacities(fluidCapacities)
                .build();
    }

    private static MetaMachine resolve(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        return MetaMachine.getMachine(level, pos);
    }

    private static RecipeLogic resolveRecipeLogic(MetaMachine machine, Level level, BlockPos pos) {
        if (level != null && pos != null) {
            RecipeLogic logic = GTCapabilityHelper.getRecipeLogic(level, pos, null);
            if (logic != null) {
                return logic;
            }
        }
        if (machine instanceof IRecipeLogicMachine recipeMachine) {
            return recipeMachine.getRecipeLogic();
        }
        return null;
    }
}
