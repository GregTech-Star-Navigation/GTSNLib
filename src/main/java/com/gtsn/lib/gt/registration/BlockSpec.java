package com.gtsn.lib.gt.registration;

import java.util.Objects;
import java.util.Optional;

/**
 * 声明式方块规格（#15）：用一处声明描述一个方块（id、硬度/爆炸抗性、亮度、是否生成方块物品、显示名），
 * 由适配层翻译为 GTCEu {@code GTRegistrate} 的方块注册链。
 *
 * <p>纯数据模型，不依赖任何 Minecraft / Forge / GTCEu 类型，可在无游戏进程下完整单测。</p>
 *
 * <pre>
 *   BlockSpec testBlock = BlockSpec.builder("mymod", "test_block")
 *           .strength(2.0F, 3.0F)
 *           .requiresCorrectToolForDrops()
 *           .lightLevel(7)
 *           .withItem(true)
 *           .build();
 * </pre>
 *
 * <p>id 与 namespace 在构建时归一化为 {@code [a-z0-9_]} 形式（与 GTCEu 的命名约定一致），因此声明可写
 * {@code "Test Block"}。</p>
 */
public final class BlockSpec {

    /** 未显式声明硬度时的默认破坏时间（对齐石头）。 */
    public static final float DEFAULT_DESTROY_TIME = 1.5F;
    /** 未显式声明爆炸抗性时的默认值（对齐石头）。 */
    public static final float DEFAULT_EXPLOSION_RESISTANCE = 6.0F;
    /** 默认不发光。 */
    public static final int DEFAULT_LIGHT_LEVEL = 0;
    /** 默认生成方块物品，使方块可被获取。 */
    public static final boolean DEFAULT_WITH_ITEM = true;
    /** 亮度下界（对齐 Minecraft 光照）。 */
    public static final int MIN_LIGHT_LEVEL = 0;
    /** 亮度上界（对齐 Minecraft 光照）。 */
    public static final int MAX_LIGHT_LEVEL = 15;

    private final String namespace;
    private final String id;
    private final float destroyTime;
    private final float explosionResistance;
    private final int lightLevel;
    private final boolean withItem;
    private final boolean requiresCorrectToolForDrops;
    private final String displayName;
    private final RegistrationHook hook;

    private BlockSpec(Builder builder) {
        this.namespace = builder.namespace;
        this.id = builder.id;
        this.destroyTime = builder.destroyTime;
        this.explosionResistance = builder.explosionResistance;
        this.lightLevel = builder.lightLevel;
        this.withItem = builder.withItem;
        this.requiresCorrectToolForDrops = builder.requiresCorrectToolForDrops;
        this.displayName = builder.displayName;
        this.hook = builder.hook;
    }

    /** 新建构建器；namespace 与 id 可包含大小写与分隔符，构建时归一化。 */
    public static Builder builder(String namespace, String id) {
        return new Builder(namespace, id);
    }

    /** 方块命名空间（已归一化），如 {@code gtsnlib}。 */
    public String namespace() {
        return namespace;
    }

    /** 方块 id 路径（已归一化），如 {@code test_block}。 */
    public String id() {
        return id;
    }

    /** 完整键 {@code namespace:id}，用于重复检测与日志。 */
    public String key() {
        return namespace + ":" + id;
    }

    /** 破坏时间（硬度）。 */
    public float destroyTime() {
        return destroyTime;
    }

    /** 爆炸抗性。 */
    public float explosionResistance() {
        return explosionResistance;
    }

    /** 亮度（{@link #MIN_LIGHT_LEVEL}..{@link #MAX_LIGHT_LEVEL}）。 */
    public int lightLevel() {
        return lightLevel;
    }

    /** 是否生成对应的方块物品（使方块可被获取）。 */
    public boolean withItem() {
        return withItem;
    }

    /** 是否要求正确工具才能掉落。 */
    public boolean requiresCorrectToolForDrops() {
        return requiresCorrectToolForDrops;
    }

    /** 自定义显示名；未声明时为空。 */
    public Optional<String> displayName() {
        return Optional.ofNullable(displayName);
    }

    /** 注册成功后的回调（可选）。 */
    public Optional<RegistrationHook> hook() {
        return Optional.ofNullable(hook);
    }

    /** {@link BlockSpec} 的流式构建器；非法值在调用点立即拒绝。 */
    public static final class Builder {

        private final String namespace;
        private final String id;
        private float destroyTime = DEFAULT_DESTROY_TIME;
        private float explosionResistance = DEFAULT_EXPLOSION_RESISTANCE;
        private int lightLevel = DEFAULT_LIGHT_LEVEL;
        private boolean withItem = DEFAULT_WITH_ITEM;
        private boolean requiresCorrectToolForDrops;
        private String displayName;
        private RegistrationHook hook;

        private Builder(String namespace, String id) {
            this.namespace = RegistrationIds.normalize("namespace", namespace);
            this.id = RegistrationIds.normalize("id", id);
        }

        /** 设置破坏时间（硬度，非负）。 */
        public Builder destroyTime(float destroyTime) {
            if (destroyTime < 0.0F) {
                throw new IllegalArgumentException("destroyTime must be non-negative: " + destroyTime);
            }
            this.destroyTime = destroyTime;
            return this;
        }

        /** 设置爆炸抗性（非负）。 */
        public Builder explosionResistance(float explosionResistance) {
            if (explosionResistance < 0.0F) {
                throw new IllegalArgumentException(
                        "explosionResistance must be non-negative: " + explosionResistance);
            }
            this.explosionResistance = explosionResistance;
            return this;
        }

        /** 一次性设置破坏时间与爆炸抗性。 */
        public Builder strength(float destroyTime, float explosionResistance) {
            return destroyTime(destroyTime).explosionResistance(explosionResistance);
        }

        /** 设置亮度（0..15）。 */
        public Builder lightLevel(int lightLevel) {
            if (lightLevel < MIN_LIGHT_LEVEL || lightLevel > MAX_LIGHT_LEVEL) {
                throw new IllegalArgumentException(
                        "lightLevel must be within " + MIN_LIGHT_LEVEL + ".." + MAX_LIGHT_LEVEL + ": " + lightLevel);
            }
            this.lightLevel = lightLevel;
            return this;
        }

        /** 是否生成方块物品。 */
        public Builder withItem(boolean withItem) {
            this.withItem = withItem;
            return this;
        }

        /** 是否要求正确工具才能掉落。 */
        public Builder requiresCorrectToolForDrops() {
            this.requiresCorrectToolForDrops = true;
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
        public BlockSpec build() {
            return new BlockSpec(this);
        }
    }
}
