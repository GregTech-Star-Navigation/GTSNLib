package com.gtsn.lib.gt.registration;

import java.util.Objects;
import java.util.Optional;

/**
 * 声明式物品规格（#15）：用一处声明描述一个物品（id、堆叠上限、是否防火、显示名），由适配层翻译为
 * GTCEu {@code GTRegistrate} 的物品注册链。
 *
 * <p>纯数据模型，不依赖任何 Minecraft / Forge / GTCEu 类型，可在无游戏进程下完整单测。</p>
 *
 * <pre>
 *   ItemSpec testItem = ItemSpec.builder("mymod", "test_item")
 *           .maxStackSize(16)
 *           .displayName("Test Item")
 *           .build();
 * </pre>
 */
public final class ItemSpec {

    /** 默认堆叠上限（对齐 Minecraft 标准物品）。 */
    public static final int DEFAULT_MAX_STACK_SIZE = 64;
    /** 堆叠上限下界。 */
    public static final int MIN_MAX_STACK_SIZE = 1;
    /** 堆叠上限上界（Minecraft 物品栏单个槽位的上限）。 */
    public static final int MAX_MAX_STACK_SIZE = 64;

    private final String namespace;
    private final String id;
    private final int maxStackSize;
    private final boolean fireResistant;
    private final String displayName;
    private final RegistrationHook hook;

    private ItemSpec(Builder builder) {
        this.namespace = builder.namespace;
        this.id = builder.id;
        this.maxStackSize = builder.maxStackSize;
        this.fireResistant = builder.fireResistant;
        this.displayName = builder.displayName;
        this.hook = builder.hook;
    }

    /** 新建构建器；namespace 与 id 可包含大小写与分隔符，构建时归一化。 */
    public static Builder builder(String namespace, String id) {
        return new Builder(namespace, id);
    }

    /** 物品命名空间（已归一化），如 {@code gtsnlib}。 */
    public String namespace() {
        return namespace;
    }

    /** 物品 id 路径（已归一化），如 {@code test_item}。 */
    public String id() {
        return id;
    }

    /** 完整键 {@code namespace:id}，用于重复检测与日志。 */
    public String key() {
        return namespace + ":" + id;
    }

    /** 堆叠上限（1..64）。 */
    public int maxStackSize() {
        return maxStackSize;
    }

    /** 是否防火（岩浆中不销毁）。 */
    public boolean fireResistant() {
        return fireResistant;
    }

    /** 自定义显示名；未声明时为空。 */
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    /** 注册成功后的回调（可选）。 */
    public Optional<RegistrationHook> hook() {
        return Optional.ofNullable(hook);
    }

    /** {@link ItemSpec} 的流式构建器；非法值在调用点立即拒绝。 */
    public static final class Builder {

        private final String namespace;
        private final String id;
        private int maxStackSize = DEFAULT_MAX_STACK_SIZE;
        private boolean fireResistant;
        private String displayName;
        private RegistrationHook hook;

        private Builder(String namespace, String id) {
            this.namespace = RegistrationIds.normalize("namespace", namespace);
            this.id = RegistrationIds.normalize("id", id);
        }

        /** 设置堆叠上限（1..64）。 */
        public Builder maxStackSize(int maxStackSize) {
            if (maxStackSize < MIN_MAX_STACK_SIZE || maxStackSize > MAX_MAX_STACK_SIZE) {
                throw new IllegalArgumentException("maxStackSize must be within "
                        + MIN_MAX_STACK_SIZE + ".." + MAX_MAX_STACK_SIZE + ": " + maxStackSize);
            }
            this.maxStackSize = maxStackSize;
            return this;
        }

        /** 是否防火。 */
        public Builder fireResistant(boolean fireResistant) {
            this.fireResistant = fireResistant;
            return this;
        }

        /** 设置显示名（非空白）。 */
        public Builder displayName(String displayName) {
            if (displayName != null && displayName.isBlank()) {
                throw new IllegalArgumentException("displayName must not be blank");
            }
            this.displayName = displayName == null ? null : displayName.trim();
            return this;
        }

        /** 设置注册成功后的回调。 */
        public Builder hook(RegistrationHook hook) {
            this.hook = Objects.requireNonNull(hook, "hook");
            return this;
        }

        /** 构建规格。 */
        public ItemSpec build() {
            return new ItemSpec(this);
        }
    }
}
