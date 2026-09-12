package com.gtsn.lib.gt.adapter;

import java.util.Objects;

/**
 * 单个声明衍生件在真实 GTCEu 运行环境中的状态（GTSN 自有视图，不含 GTCEu 类型）。
 *
 * <p>由适配层在运行期实时查询 GTCEu 得到：物品是否已生成、对应矿词为何。供 {@code /gtsnlib}
 * 命令与 GameTest 作为“衍生件与矿词生成正确”的证据。</p>
 *
 * @param part          声明使用的部件键（如 {@code ingot}）
 * @param itemId        期望的 GT 物品路径（如 {@code star_alloy_ingot}）
 * @param oreTag        该部件对应的矿词（如 {@code forge:ingots/star_alloy}）；无则为空串
 * @param itemGenerated GTCEu 是否已真正生成该部件的物品
 */
public record GtPartStatus(String part, String itemId, String oreTag, boolean itemGenerated) {

    public GtPartStatus {
        Objects.requireNonNull(part, "part");
        Objects.requireNonNull(itemId, "itemId");
        Objects.requireNonNull(oreTag, "oreTag");
    }
}
