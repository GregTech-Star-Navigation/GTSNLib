package com.gtsn.lib.compat.mekanism;

import java.util.List;
import java.util.Objects;

/**
 * 一次化学物质注册的结果视图（GTSN 自有类型，不含任何 Mekanism 类型），供命令、日志与注册钩子共用（#14）。
 *
 * @param id                声明 id 路径（已归一化，如 {@code test_chemical}）
 * @param namespace         命名空间（如 {@code gtsnlib}）
 * @param key               声明完整键 {@code namespace:id}
 * @param kind              化学种类
 * @param registryId        目标 Mekanism 注册表资源位置（如 {@code mekanism:gas}）
 * @param tint              RGB 颜色（0x000000..0xFFFFFF）
 * @param hidden            是否在 JEI 等界面隐藏
 * @param resourceLocations 真实注册表条目资源位置；浆液为 {@code dirty_}/{@code clean_} 两条
 */
public record ChemicalRegistration(
        String id,
        String namespace,
        String key,
        ChemicalKind kind,
        String registryId,
        int tint,
        boolean hidden,
        List<String> resourceLocations) {

    public ChemicalRegistration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(registryId, "registryId");
        Objects.requireNonNull(resourceLocations, "resourceLocations");
        resourceLocations = List.copyOf(resourceLocations);
        if (resourceLocations.isEmpty()) {
            throw new IllegalArgumentException("chemical registration must expose at least one resource location: " + key);
        }
    }

    /** 首个（主）真实注册表条目资源位置。 */
    public String resourceLocation() {
        return resourceLocations.get(0);
    }

    /** 是否存在多个真实注册表条目（浆液 dirty/clean）。 */
    public boolean hasMultipleResourceLocations() {
        return resourceLocations.size() > 1;
    }
}
