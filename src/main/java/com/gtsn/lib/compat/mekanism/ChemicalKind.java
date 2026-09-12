package com.gtsn.lib.compat.mekanism;

import java.util.Locale;
import java.util.Objects;

/**
 * 声明式化学物质的种类（#14）：气体 / 浆液 / 灌注类型 / 色素。
 *
 * <p>这是 GTSNLib 自有的稳定词汇（与 Mekanism 类型无关）。{@code com.gtsn.lib.integration.mekanism}
 * 内的隔离翻译层负责把每种种类映射到 Mekanism 的注册表与构建器，从而保证本枚举不耦合上游。</p>
 */
public enum ChemicalKind {

    /** 气体（Mekanism {@code Gas}）。 */
    GAS("gas", "mekanism:gas"),
    /** 浆液（Mekanism {@code Slurry}，成对生成 dirty/clean 两条）。 */
    SLURRY("slurry", "mekanism:slurry"),
    /** 灌注类型（Mekanism {@code InfuseType}）。 */
    INFUSE_TYPE("infuse_type", "mekanism:infuse_type"),
    /** 色素（Mekanism {@code Pigment}）。 */
    PIGMENT("pigment", "mekanism:pigment");

    private final String key;
    private final String registryId;

    ChemicalKind(String key, String registryId) {
        this.key = key;
        this.registryId = registryId;
    }

    /** 声明使用的稳定小写键，例如 {@code gas}。 */
    public String key() {
        return key;
    }

    /** 目标 Mekanism 注册表的资源位置，例如 {@code mekanism:gas}。 */
    public String registryId() {
        return registryId;
    }

    /**
     * 按声明键解析化学种类（大小写不敏感）。
     *
     * @throws IllegalArgumentException 键为空或未知
     */
    public static ChemicalKind fromKey(String key) {
        Objects.requireNonNull(key, "key");
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (ChemicalKind kind : values()) {
            if (kind.key.equals(normalized)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("unknown chemical kind: " + key);
    }
}
