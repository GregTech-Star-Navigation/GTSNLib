package com.gtsn.lib.gt.registration;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 一次材料注册的结果视图（GTSN 自有类型，不含任何 GTCEu 类型），供命令、日志与配方钩子共用。
 *
 * @param id               材料 id 路径（已归一化，如 {@code stellar_alloy}）
 * @param namespace        材料命名空间（如 {@code gtsnlib}）
 * @param resourceLocation 完整资源位置（{@code namespace:id}）
 * @param derivedItems     衍生件映射：部件键（如 {@code ingot}）→ 生成物品路径（如 {@code stellar_alloy_ingot}）
 * @param oreTags          该材料生成的矿词（ore dictionary tag）字符串
 */
public record MaterialRegistration(
        String id,
        String namespace,
        String resourceLocation,
        Map<String, String> derivedItems,
        List<String> oreTags) {

    public MaterialRegistration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(resourceLocation, "resourceLocation");
        derivedItems = Map.copyOf(Objects.requireNonNull(derivedItems, "derivedItems"));
        oreTags = List.copyOf(Objects.requireNonNull(oreTags, "oreTags"));
    }

    /** 按部件键查询衍生件物品路径。 */
    public Optional<String> derivedItem(String partKey) {
        return Optional.ofNullable(derivedItems.get(partKey));
    }

    /** 是否生成了给定矿词。 */
    public boolean hasOreTag(String tag) {
        return oreTags.contains(tag);
    }
}
