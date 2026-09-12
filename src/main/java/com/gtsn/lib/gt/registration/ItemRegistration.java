package com.gtsn.lib.gt.registration;

import java.util.Objects;

/**
 * 一次物品注册的结果视图（#15，GTSN 自有类型，不含任何 GTCEu 类型），供命令、日志与钩子共用。
 *
 * @param id               物品 id 路径（已归一化，如 {@code test_item}）
 * @param namespace        物品命名空间（如 {@code gtsnlib}）
 * @param resourceLocation 完整资源位置（{@code namespace:id}）
 * @param maxStackSize     声明的堆叠上限
 */
public record ItemRegistration(
        String id,
        String namespace,
        String resourceLocation,
        int maxStackSize) {

    public ItemRegistration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(resourceLocation, "resourceLocation");
    }
}
