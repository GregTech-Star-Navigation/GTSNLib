package com.gtsn.lib.gt.adapter;

import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.machine.MetaMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * GT 机器调试注入（ADR-0005：GTCEu import 只允许出现在 {@code gt/adapter}）。
 *
 * <p>{@link GtMachineSnapshots} 严格只读；本类是其显式的、唯一的写入口，只为开发调试命令与自动测试
 * 提供「服务端写入 → LDLib {@code @DescSynced} 客户端镜像」的同步证据。生产界面路径不使用本类。</p>
 *
 * <p>注入的是坐标处 GT 机器能量容器的存量（经 {@link GTCapabilityHelper#getEnergyContainer}，
 * 可空安全）：无机器 / 无能量容器时返回 0，不抛异常。写入后调用 {@link MetaMachine#onChanged()}
 * 让 LDLib 把带 {@code @DescSynced} 的字段推送给客户端——{@code NotifiableEnergyContainer} 自身
 * 的 {@code changeEnergy} 只改字段、不标脏，故必须显式触发，否则客户端镜像永远停在旧值。</p>
 */
public final class GtMachineDebug {

    private GtMachineDebug() {
    }

    /**
     * 调整坐标处机器能量容器的存量（{@code deltaEU > 0} 注入，{@code < 0} 移除，按容量 / 零下限钳制）。
     *
     * @return 实际变化量（正为注入、负为移除）；无机器 / 无能量容器时为 0
     */
    public static long charge(Level level, BlockPos pos, long deltaEU) {
        if (level == null || pos == null) {
            return 0L;
        }
        IEnergyContainer energy = GTCapabilityHelper.getEnergyContainer(level, pos, null);
        if (energy == null) {
            return 0L;
        }
        long changed = energy.changeEnergy(deltaEU);
        if (changed != 0L && !level.isClientSide) {
            MetaMachine machine = MetaMachine.getMachine(level, pos);
            if (machine != null) {
                machine.onChanged();
            }
        }
        return changed;
    }
}

