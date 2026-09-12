package com.gtsn.lib.gt.registration;

import java.util.Objects;
import java.util.Optional;

/**
 * 声明式机器规格（#15）：用一处声明描述一台机器（id、能量等级 tier、显示名），由适配层翻译为 GTCEu
 * {@code GTRegistrate} 的机器注册链（{@code MachineBuilder}）。
 *
 * <p>纯数据模型，不依赖任何 Minecraft / Forge / GTCEu 类型，可在无游戏进程下完整单测。</p>
 *
 * <pre>
 *   MachineSpec testMachine = MachineSpec.builder("mymod", "test_machine")
 *           .tier(2)
 *           .displayName("Test Machine")
 *           .build();
 * </pre>
 *
 * <p>显示名未显式声明时由 id 推导（{@code test_machine} → {@code Test Machine}）。</p>
 */
public final class MachineSpec {

    /** 默认能量等级（LV，对齐 GTCEu {@code GTValues.LV}）。 */
    public static final int DEFAULT_TIER = 1;
    /** 等级下界（ULV）。 */
    public static final int MIN_TIER = 0;
    /** 等级上界（MAX，对齐 GTCEu tier 计数）。 */
    public static final int MAX_TIER = 14;

    private final String namespace;
    private final String id;
    private final int tier;
    private final String displayName;
    private final RegistrationHook hook;

    private MachineSpec(Builder builder) {
        this.namespace = builder.namespace;
        this.id = builder.id;
        this.tier = builder.tier;
        this.displayName = builder.displayName;
        this.hook = builder.hook;
    }

    /** 新建构建器；namespace 与 id 可包含大小写与分隔符，构建时归一化。 */
    public static Builder builder(String namespace, String id) {
        return new Builder(namespace, id);
    }

    /** 机器命名空间（已归一化），如 {@code gtsnlib}。 */
    public String namespace() {
        return namespace;
    }

    /** 机器 id 路径（已归一化），如 {@code test_machine}。 */
    public String id() {
        return id;
    }

    /** 完整键 {@code namespace:id}，用于重复检测与日志。 */
    public String key() {
        return namespace + ":" + id;
    }

    /** 能量等级（{@link #MIN_TIER}..{@link #MAX_TIER}）。 */
    public int tier() {
        return tier;
    }

    /** 显示名（未显式声明时由 id 推导）。 */
    public String displayName() {
        return displayName;
    }

    /** 注册成功后的回调（可选）。 */
    public Optional<RegistrationHook> hook() {
        return Optional.ofNullable(hook);
    }

    /** {@link MachineSpec} 的流式构建器；非法值在调用点立即拒绝。 */
    public static final class Builder {

        private final String namespace;
        private final String id;
        private int tier = DEFAULT_TIER;
        private String displayName;
        private RegistrationHook hook;

        private Builder(String namespace, String id) {
            this.namespace = RegistrationIds.normalize("namespace", namespace);
            this.id = RegistrationIds.normalize("id", id);
        }

        /** 设置能量等级（0..14）。 */
        public Builder tier(int tier) {
            if (tier < MIN_TIER || tier > MAX_TIER) {
                throw new IllegalArgumentException(
                        "tier must be within " + MIN_TIER + ".." + MAX_TIER + ": " + tier);
            }
            this.tier = tier;
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

        /** 构建规格；显示名缺省时由 id 推导。 */
        public MachineSpec build() {
            if (displayName == null) {
                displayName = RegistrationIds.defaultDisplayName(id);
            }
            return new MachineSpec(this);
        }
    }
}
