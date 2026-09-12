package com.gtsn.lib.gt.registration;

import java.util.Locale;
import java.util.Objects;

/**
 * 声明式流体的物态（#13）：液体 / 气体 / 等离子体。
 *
 * <p>这是 GTSNLib 自有的稳定词汇（与 GTCEu 类型无关）。{@link com.gtsn.lib.gt.adapter} 内的适配层
 * 负责把每种物态映射到 GTCEu 的 {@code FluidStorageKey} 与 {@code FluidState}，从而保证本枚举
 * 不耦合上游。</p>
 */
public enum FluidState {

    /** 液体。 */
    LIQUID("liquid"),
    /** 气体。 */
    GAS("gas"),
    /** 等离子体。 */
    PLASMA("plasma");

    private final String key;

    FluidState(String key) {
        this.key = key;
    }

    /** 声明使用的稳定小写键，例如 {@code liquid}。 */
    public String key() {
        return key;
    }

    /**
     * 按声明键解析物态（大小写不敏感）。
     *
     * @throws IllegalArgumentException 键为空或未知
     */
    public static FluidState fromKey(String key) {
        Objects.requireNonNull(key, "key");
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (FluidState state : values()) {
            if (state.key.equals(normalized)) {
                return state;
            }
        }
        throw new IllegalArgumentException("unknown fluid state: " + key);
    }
}
