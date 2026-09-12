package com.gtsn.lib.gt.adapter;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.TieredEnergyMachine;

/**
 * 通用注册简化层（#15）用于游戏内验证的最小机器实现。
 *
 * <p>只承载「一台可通过 {@code GTRegistrate#machine} 注册、可被 GTCEu 机器注册表查询、且带最小能量容器」
 * 的行为，不含任何配方逻辑或 LDLib 界面。其方块/物品/方块实体类型均由 GTCEu 的标准机器模板生成。</p>
 *
 * <p>继承 {@link TieredEnergyMachine}（tier 1 / LV）以便 #22 机器界面桥接的 GameTest 能验证真实能量读取
 * （{@code getEnergyCapacity() > 0}）；能量容器由 GTCEu 的 {@code NotifiableEnergyContainer} 提供并
 * {@code @DescSynced} 同步到客户端。</p>
 *
 * <p>本类位于适配层，因此允许直接引用 GTCEu 类型（ADR-0005）。</p>
 */
public class GtsnTestMachine extends TieredEnergyMachine {

    public static final int TIER = 1;

    public GtsnTestMachine(IMachineBlockEntity holder) {
        super(holder, TIER);
    }
}

