package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.render.SlotIcon;

import java.util.List;
import java.util.Objects;

/**
 * 机器状态面板的 MC-free 视图模型（#22 评审 F1-3）：把 GT 快照里的 MC 公共类型
 * （{@code ItemStack} / {@code FluidStack}）翻译为与渲染无关的展示载荷，使
 * {@code com.gtsn.lib.ui.widget} 组件层不含任何 {@code net.minecraft} 依赖。
 *
 * <p>槽位用 {@link Slot}（索引 + 图标无关载荷 {@link SlotIcon}，空槽为 {@code null}）；流体罐用
 * {@link Tank}（存量 / 容量 / 名称）。{@code ItemStack} / {@code FluidStack} → 本视图的翻译由客户端
 * binder（{@code com.gtsn.lib.ui.client.MachineStatusViewBinder}）完成，故组件可在无游戏环境测试。</p>
 *
 * <p>依赖 mod 可直接构造本视图（例如用程序化 {@link SlotIcon}）来复用 {@link MachineStatusPanel}，
 * 无需接触 MC / GT 类型。</p>
 */
public final class MachineStatusView {

    private final String machineId;
    private final String status;
    private final long energyStored;
    private final long energyCapacity;
    private final int progress;
    private final int maxProgress;
    private final boolean working;
    private final List<Slot> itemSlots;
    private final List<Tank> tanks;

    private MachineStatusView(Builder builder) {
        this.machineId = builder.machineId;
        this.status = builder.status;
        this.energyStored = Math.max(0L, builder.energyStored);
        this.energyCapacity = Math.max(0L, builder.energyCapacity);
        this.progress = Math.max(0, builder.progress);
        this.maxProgress = Math.max(0, builder.maxProgress);
        this.working = builder.working;
        this.itemSlots = List.copyOf(builder.itemSlots);
        this.tanks = List.copyOf(builder.tanks);
    }

    public String machineId() {
        return machineId;
    }

    /** 状态文本（无配方逻辑时通常为空串 → 面板按空文本处理）。 */
    public String status() {
        return status;
    }

    public long energyStored() {
        return energyStored;
    }

    public long energyCapacity() {
        return energyCapacity;
    }

    public int progress() {
        return progress;
    }

    public int maxProgress() {
        return maxProgress;
    }

    public boolean working() {
        return working;
    }

    /** 物品槽（按索引升序；空槽 {@link Slot#icon()} 为 {@code null}）。 */
    public List<Slot> itemSlots() {
        return itemSlots;
    }

    /** 流体罐（按罐索引顺序）。 */
    public List<Tank> tanks() {
        return tanks;
    }

    public static Builder builder(String machineId) {
        return new Builder(machineId);
    }

    /** 槽位描述符：索引 + 图标无关载荷（空槽为 {@code null}）。 */
    public record Slot(int index, SlotIcon icon) {
        public Slot {
            if (index < 0) {
                throw new IllegalArgumentException("slot index must be non-negative: " + index);
            }
        }

        public boolean isEmpty() {
            return icon == null;
        }
    }

    /** 流体罐描述符：存量 / 容量 / 显示名（负值钳制，{@code null} 名归一为空串）。 */
    public record Tank(long amount, long capacity, String name) {
        public Tank {
            amount = Math.max(0L, amount);
            capacity = Math.max(0L, capacity);
            name = name == null ? "" : name;
        }
    }

    /** 视图构建器：未设置字段取安全默认值（0 / 空列表 / 空状态）。 */
    public static final class Builder {

        private final String machineId;
        private String status = "";
        private long energyStored;
        private long energyCapacity;
        private int progress;
        private int maxProgress;
        private boolean working;
        private List<Slot> itemSlots = List.of();
        private List<Tank> tanks = List.of();

        private Builder(String machineId) {
            Objects.requireNonNull(machineId, "machineId");
            if (machineId.isBlank()) {
                throw new IllegalArgumentException("machineId must not be blank");
            }
            this.machineId = machineId;
        }

        public Builder status(String status) {
            this.status = status == null ? "" : status;
            return this;
        }

        public Builder energy(long stored, long capacity) {
            this.energyStored = stored;
            this.energyCapacity = capacity;
            return this;
        }

        public Builder progress(int progress, int maxProgress) {
            this.progress = progress;
            this.maxProgress = maxProgress;
            return this;
        }

        public Builder working(boolean working) {
            this.working = working;
            return this;
        }

        public Builder itemSlots(List<Slot> itemSlots) {
            this.itemSlots = copyNonNull(itemSlots, "itemSlots");
            return this;
        }

        public Builder tanks(List<Tank> tanks) {
            this.tanks = copyNonNull(tanks, "tanks");
            return this;
        }

        public MachineStatusView build() {
            return new MachineStatusView(this);
        }

        private static <T> List<T> copyNonNull(List<T> values, String name) {
            Objects.requireNonNull(values, name);
            for (T value : values) {
                Objects.requireNonNull(value, name + " element");
            }
            return List.copyOf(values);
        }
    }
}
