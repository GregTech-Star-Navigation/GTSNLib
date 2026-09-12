package com.gtsn.lib.gt.adapter;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MetaMachine;

/**
 * 通用注册简化层（#15）用于游戏内验证的最小机器实现。
 *
 * <p>只承载「一台可通过 {@code GTRegistrate#machine} 注册、可被 GTCEu 机器注册表查询」的最小行为，
 * 不含任何配方逻辑或界面。其方块/物品/方块实体类型均由 GTCEu 的标准机器模板生成。</p>
 *
 * <p>本类位于适配层，因此允许直接引用 GTCEu 类型（ADR-0005）。</p>
 */
public class GtsnTestMachine extends MetaMachine {

    public GtsnTestMachine(IMachineBlockEntity holder) {
        super(holder);
    }
}
