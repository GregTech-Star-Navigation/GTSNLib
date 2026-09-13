package com.gtsn.lib.ui.client;

import com.gtsn.lib.api.GtMachineSnapshot;
import com.gtsn.lib.ui.render.ItemStackIcon;
import com.gtsn.lib.ui.render.SlotIcon;
import com.gtsn.lib.ui.widget.MachineStatusView;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * {@link MachineStatusView} 的客户端 binder（#22 评审 F1-3）：把 {@link GtMachineSnapshot}
 * 里的 MC 公共类型（{@link ItemStack} / {@link FluidStack}）翻译为 MC-free 视图模型——
 * 物品槽 → {@code ItemStackIcon}（真实物品图标），流体罐 → 存量 / 容量 / 显示名。
 *
 * <p>翻译只发生在客户端 binder 层，{@code ui.widget} 组件层因此不含任何 {@code net.minecraft} 依赖。
 * 客户端专用类：专职服务端不得加载（类加载纪律，见 ADR-0003/0004）。</p>
 */
public final class MachineStatusViewBinder {

    private MachineStatusViewBinder() {
    }

    /** 从 GT 机器快照构建面板视图（空物品槽 / 空罐保留为占位，不产生图标 / 名称）。 */
    public static MachineStatusView bind(GtMachineSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");

        List<MachineStatusView.Slot> slots = new ArrayList<>();
        for (int index = 0; index < snapshot.itemSlots().size(); index++) {
            ItemStack stack = snapshot.itemSlots().get(index);
            SlotIcon icon = stack == null || stack.isEmpty() ? null : ItemStackIcon.of(stack);
            slots.add(new MachineStatusView.Slot(index, icon));
        }

        List<MachineStatusView.Tank> tanks = new ArrayList<>();
        for (int index = 0; index < snapshot.fluidTanks().size(); index++) {
            FluidStack fluid = snapshot.fluidTanks().get(index);
            long amount = fluid == null ? 0L : fluid.getAmount();
            String name = fluid == null || fluid.isEmpty() ? "" : fluid.getDisplayName().getString();
            tanks.add(new MachineStatusView.Tank(amount, snapshot.fluidCapacity(index), name));
        }

        return MachineStatusView.builder(snapshot.machineId())
                .status(snapshot.status())
                .energy(snapshot.energyStored(), snapshot.energyCapacity())
                .progress(snapshot.progress(), snapshot.maxProgress())
                .working(snapshot.working())
                .itemSlots(slots)
                .tanks(tanks)
                .build();
    }
}
