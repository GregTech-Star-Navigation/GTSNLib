package com.gtsn.lib.gt.registration;

import java.util.Objects;

/**
 * 声明式材料的一个化学组分（组分材料 id + 数量），用于化合物材料。
 *
 * @param material 组分材料 id（GT 材料名，如 {@code iron}；自定义材料用 {@code namespace:path}）
 * @param amount   数量，必须为正
 */
public record MaterialComponent(String material, int amount) {

    public MaterialComponent {
        Objects.requireNonNull(material, "material");
        if (material.isBlank()) {
            throw new IllegalArgumentException("material must not be blank");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive: " + amount);
        }
    }
}
