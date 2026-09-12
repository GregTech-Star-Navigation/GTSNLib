package com.gtsn.lib.ui.sync;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 同步槽位注册表：声明一个菜单的同步值布局，为槽位分配后端索引，供服务端写 / 客户端读共用。
 *
 * <p>用法：{@code SyncLayout.builder()} 声明全部槽位（获取类型化句柄），最后 {@code build()} 冻结。
 * 每个槽位占一个 int 数据槽；同一布局在两端的菜单构造中分别实例化后端（{@link MenuSync#attach}）。</p>
 *
 * <p>槽位默认值在声明时即按编解码器规则归一（如越界值钳制），保证默认值与写入后的读取值一致。</p>
 */
public final class SyncLayout {

    private final List<SyncSlot<?>> slots;
    private final Map<String, SyncSlot<?>> byName;

    private SyncLayout(List<SyncSlot<?>> slots) {
        this.slots = List.copyOf(slots);
        Map<String, SyncSlot<?>> map = new HashMap<>();
        for (SyncSlot<?> slot : slots) {
            map.put(slot.name(), slot);
        }
        this.byName = Map.copyOf(map);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** 槽位数量（= 后端需要的 int 数据槽数量）。 */
    public int slotCount() {
        return slots.size();
    }

    /** 按声明顺序的全部槽位（不可变）。 */
    public List<SyncSlot<?>> slots() {
        return slots;
    }

    /** 按索引取槽位。 */
    public SyncSlot<?> slotAt(int index) {
        if (index < 0 || index >= slots.size()) {
            throw new IndexOutOfBoundsException("sync slot index out of range: " + index);
        }
        return slots.get(index);
    }

    /** 按名称取槽位。 */
    public SyncSlot<?> slot(String name) {
        Objects.requireNonNull(name, "name");
        SyncSlot<?> slot = byName.get(name);
        if (slot == null) {
            throw new IllegalArgumentException("unknown sync slot: " + name);
        }
        return slot;
    }

    /** 布局构建器：声明槽位并冻结为 {@link SyncLayout}。 */
    public static final class Builder {

        private final List<SyncSlot<?>> slots = new ArrayList<>();
        private final Set<String> names = new HashSet<>();
        private boolean built;

        private Builder() {
        }

        /** 声明一个槽位并返回类型化句柄；索引按调用顺序分配，名称在布局内唯一。 */
        public <T> SyncSlot<T> slot(String name, SyncCodec<T> codec, T defaultValue) {
            if (built) {
                throw new IllegalStateException("layout already built; declare slots before build()");
            }
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(codec, "codec");
            Objects.requireNonNull(defaultValue, "defaultValue");
            if (!names.add(name)) {
                throw new IllegalArgumentException("duplicate sync slot name: " + name);
            }
            SyncSlot<T> slot = new SyncSlot<>(slots.size(), name, codec, defaultValue);
            slots.add(slot);
            return slot;
        }

        /** 冻结布局；之后不可再声明槽位。 */
        public SyncLayout build() {
            if (built) {
                throw new IllegalStateException("layout already built");
            }
            built = true;
            return new SyncLayout(slots);
        }
    }
}
