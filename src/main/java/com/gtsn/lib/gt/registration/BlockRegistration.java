package com.gtsn.lib.gt.registration;

import java.util.Objects;

/**
 * 一次方块注册的结果视图（#15，GTSN 自有类型，不含任何 GTCEu 类型），供命令、日志与钩子共用。
 *
 * @param id               方块 id 路径（已归一化，如 {@code test_block}）
 * @param namespace        方块命名空间（如 {@code gtsnlib}）
 * @param resourceLocation 完整资源位置（{@code namespace:id}）
 * @param itemResourceLocation 方块物品的资源位置；未生成方块物品时为空串
 */
public record BlockRegistration(
        String id,
        String namespace,
        String resourceLocation,
        String itemResourceLocation) {

    public BlockRegistration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(resourceLocation, "resourceLocation");
        Objects.requireNonNull(itemResourceLocation, "itemResourceLocation");
    }

    /** 是否生成了可获取的方块物品。 */
    public boolean hasItem() {
        return !itemResourceLocation.isEmpty();
    }
}
