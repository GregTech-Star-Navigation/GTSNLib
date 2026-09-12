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
 * @param fluids           材料流体形态映射：物态键（如 {@code gas}）→ GT 流体资源位置；无则为空表
 */
public record MaterialRegistration(
        String id,
        String namespace,
        String resourceLocation,
        Map<String, String> derivedItems,
        List<String> oreTags,
        Map<String, String> fluids) {

    public MaterialRegistration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(resourceLocation, "resourceLocation");
        derivedItems = Map.copyOf(Objects.requireNonNull(derivedItems, "derivedItems"));
        oreTags = List.copyOf(Objects.requireNonNull(oreTags, "oreTags"));
        fluids = Map.copyOf(Objects.requireNonNull(fluids, "fluids"));
    }

    /** 兼容无流体形态的构造（#12 调用点）。 */
    public MaterialRegistration(
            String id,
            String namespace,
            String resourceLocation,
            Map<String, String> derivedItems,
            List<String> oreTags) {
        this(id, namespace, resourceLocation, derivedItems, oreTags, Map.of());
    }

    /** 按部件键查询衍生件物品路径。 */
    public Optional<String> derivedItem(String partKey) {
        return Optional.ofNullable(derivedItems.get(partKey));
    }

    /** 是否生成了给定矿词。 */
    public boolean hasOreTag(String tag) {
        return oreTags.contains(tag);
    }

    /** 按物态键（{@code liquid}/{@code gas}/{@code plasma}）查询材料流体资源位置。 */
    public Optional<String> fluid(String stateKey) {
        return Optional.ofNullable(fluids.get(stateKey));
    }

    /** 是否注册了给定物态的材料流体。 */
    public boolean hasFluid(String stateKey) {
        return fluids.containsKey(stateKey);
    }
}
