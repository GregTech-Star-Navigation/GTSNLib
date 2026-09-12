package com.gtsn.lib.api;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;
import java.util.Objects;

/**
 * GT 机器只读快照（GT-free）：在某一时刻从 GTCEu 机器读出的展示态数据。
 *
 * <p>本类位于 {@code com.gtsn.lib.api}，<b>不得</b>引用任何 GTCEu 类型——GT 侧的读取与翻译统一收敛在
 * {@code com.gtsn.lib.gt.adapter}（ADR-0005）。依赖 mod 可基于本模型用 GTSN UI 组装机器仪表盘 /
 * 监控面板，而无需接触 GTCEu 或 LDLib。</p>
 *
 * <p>只读语义：快照是值对象，构造后不可变；列表访问器返回不可修改副本。它不持有机器引用，
 * 因此不会在快照存活期间反向操作机器，也不参与双向交互（本轮只读展示）。</p>
 *
 * <p>比例计算（能量 / 配方进度 / 流体容量）在此纯算术完成，可在无游戏环境的单元测试中验证。</p>
 */
public final class GtMachineSnapshot {

    /** 无法判断机器状态时的状态文本。 */
    public static final String STATUS_UNKNOWN = "UNKNOWN";

    private final String machineId;
    private final int tier;
    private final String status;
    private final long energyStored;
    private final long energyCapacity;
    private final long inputVoltage;
    private final int progress;
    private final int maxProgress;
    private final boolean working;
    private final List<ItemStack> itemSlots;
    private final List<FluidStack> fluidTanks;
    private final List<Long> fluidTankCapacities;

    private GtMachineSnapshot(Builder builder) {
        this.machineId = builder.machineId;
        this.tier = Math.max(0, builder.tier);
        this.status = builder.status;
        this.energyStored = Math.max(0L, builder.energyStored);
        this.energyCapacity = Math.max(0L, builder.energyCapacity);
        this.inputVoltage = Math.max(0L, builder.inputVoltage);
        this.progress = Math.max(0, builder.progress);
        this.maxProgress = Math.max(0, builder.maxProgress);
        this.working = builder.working;
        this.itemSlots = List.copyOf(builder.itemSlots);
        this.fluidTanks = List.copyOf(builder.fluidTanks);
        this.fluidTankCapacities = List.copyOf(builder.fluidTankCapacities);
    }

    /** 机器资源位置字符串，例如 {@code gtsnlib:test_machine}。 */
    public String machineId() {
        return machineId;
    }

    /** GT 机器 tier（电压等级）。 */
    public int tier() {
        return tier;
    }

    /** 状态文本（配方逻辑状态，未知时为 {@link #STATUS_UNKNOWN}）。 */
    public String status() {
        return status;
    }

    public long energyStored() {
        return energyStored;
    }

    public long energyCapacity() {
        return energyCapacity;
    }

    public long inputVoltage() {
        return inputVoltage;
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

    /** 物品槽内容（按槽位索引顺序；空槽为 {@link ItemStack#EMPTY}）。 */
    public List<ItemStack> itemSlots() {
        return itemSlots;
    }

    /** 流体罐内容（按罐索引顺序；空罐为 {@link FluidStack#EMPTY}）。 */
    public List<FluidStack> fluidTanks() {
        return fluidTanks;
    }

    /** 与 {@link #fluidTanks()} 对齐的罐容量（可选附加信息，缺省时为空）。 */
    public List<Long> fluidTankCapacities() {
        return fluidTankCapacities;
    }

    /** 能量填充比例 [0, 1]；容量未知（<=0）时为 0。 */
    public double energyRatio() {
        return ratio(energyStored, energyCapacity);
    }

    /** 配方进度比例 [0, 1]；无配方（maxProgress<=0）时为 0。 */
    public double progressRatio() {
        return ratio(progress, maxProgress);
    }

    /** 是否存在能量容器（容量 > 0）。 */
    public boolean hasEnergy() {
        return energyCapacity > 0L;
    }

    /** 是否存在配方进度（maxProgress > 0）。 */
    public boolean hasRecipeProgress() {
        return maxProgress > 0;
    }

    /**
     * 第 {@code index} 个流体罐的容量；容量列表不足或越界时回退为 0（调用方可用
     * {@link #fluidTanks()} 的存量自行展示）。
     */
    public long fluidCapacity(int index) {
        if (index < 0 || index >= fluidTankCapacities.size()) {
            return 0L;
        }
        return Math.max(0L, fluidTankCapacities.get(index));
    }

    private static double ratio(long value, long total) {
        if (total <= 0L) {
            return 0.0;
        }
        double ratio = (double) value / (double) total;
        return Math.max(0.0, Math.min(1.0, ratio));
    }

    public static Builder builder(String machineId) {
        return new Builder(machineId);
    }

    /** 空快照（未知机器 / 读取失败时的保底值，仅用于占位展示）。 */
    public static GtMachineSnapshot empty() {
        return builder("gtsnlib:unknown").status(STATUS_UNKNOWN).build();
    }

    /** 快照构建器：未设置的字段取安全默认值（0 / 空 / {@link #STATUS_UNKNOWN}）。 */
    public static final class Builder {

        private final String machineId;
        private int tier;
        private String status = STATUS_UNKNOWN;
        private long energyStored;
        private long energyCapacity;
        private long inputVoltage;
        private int progress;
        private int maxProgress;
        private boolean working;
        private List<ItemStack> itemSlots = List.of();
        private List<FluidStack> fluidTanks = List.of();
        private List<Long> fluidTankCapacities = List.of();

        private Builder(String machineId) {
            Objects.requireNonNull(machineId, "machineId");
            if (machineId.isBlank()) {
                throw new IllegalArgumentException("machineId must not be blank");
            }
            this.machineId = machineId;
        }

        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

        public Builder status(String status) {
            this.status = status == null || status.isBlank() ? STATUS_UNKNOWN : status;
            return this;
        }

        public Builder energy(long stored, long capacity) {
            this.energyStored = stored;
            this.energyCapacity = capacity;
            return this;
        }

        public Builder inputVoltage(long inputVoltage) {
            this.inputVoltage = inputVoltage;
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

        public Builder itemSlots(List<ItemStack> itemSlots) {
            this.itemSlots = copyNonNull(itemSlots, "itemSlots");
            return this;
        }

        public Builder fluidTanks(List<FluidStack> fluidTanks) {
            this.fluidTanks = copyNonNull(fluidTanks, "fluidTanks");
            return this;
        }

        public Builder fluidTankCapacities(List<Long> fluidTankCapacities) {
            this.fluidTankCapacities = copyNonNull(fluidTankCapacities, "fluidTankCapacities");
            return this;
        }

        public GtMachineSnapshot build() {
            return new GtMachineSnapshot(this);
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
