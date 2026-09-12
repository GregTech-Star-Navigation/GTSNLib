package com.gtsn.lib.gt.registration;

import java.util.Locale;
import java.util.Objects;

/**
 * 通用注册的种类（#15）：方块 / 物品 / 机器。
 *
 * <p>这是 GTSNLib 自有的稳定词汇。{@link #registryName()} 给出该种类最终落入的注册表名
 * （方块/物品为 Minecraft 注册表，机器为 GTCEu 机器注册表），但本枚举不链接任何上游类型，
 * 适配层负责把注册名映射为真实注册表查询。</p>
 */
public enum RegistrationKind {

    /** 方块（{@code minecraft:block}）。 */
    BLOCK("block", "minecraft:block"),
    /** 物品（{@code minecraft:item}）。 */
    ITEM("item", "minecraft:item"),
    /** 机器（{@code gtceu:machine}）。 */
    MACHINE("machine", "gtceu:machine");

    private final String key;
    private final String registryName;

    RegistrationKind(String key, String registryName) {
        this.key = key;
        this.registryName = registryName;
    }

    /** 声明使用的稳定小写键，例如 {@code block}。 */
    public String key() {
        return key;
    }

    /** 该种类最终落入的注册表名，例如 {@code minecraft:block}。 */
    public String registryName() {
        return registryName;
    }

    /**
     * 按声明键解析种类（大小写不敏感）。
     *
     * @throws IllegalArgumentException 键为空或未知
     */
    public static RegistrationKind fromKey(String key) {
        Objects.requireNonNull(key, "key");
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (RegistrationKind kind : values()) {
            if (kind.key.equals(normalized)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("unknown registration kind: " + key);
    }
}
