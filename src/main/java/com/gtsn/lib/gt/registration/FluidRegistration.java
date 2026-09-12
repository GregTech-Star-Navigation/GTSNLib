package com.gtsn.lib.gt.registration;

import java.util.Objects;
import java.util.Optional;

/**
 * 一次流体注册的结果视图（GTSN 自有类型，不含任何 GTCEu 类型），供命令、日志与注册钩子共用。
 *
 * @param id               流体 id 路径（已归一化，如 {@code star_alloy_gas}）
 * @param namespace        流体命名空间（如 {@code gtsnlib}）
 * @param resourceLocation 完整资源位置（{@code namespace:id}）
 * @param state            物态
 * @param materialKey      关联材料的完整键（{@code namespace:id}）；独立流体为空串
 * @param fluidId          真实 GTCEu 流体资源位置（{@code namespace:path}）
 */
public record FluidRegistration(
        String id,
        String namespace,
        String resourceLocation,
        FluidState state,
        String materialKey,
        String fluidId) {

    public FluidRegistration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(resourceLocation, "resourceLocation");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(materialKey, "materialKey");
        Objects.requireNonNull(fluidId, "fluidId");
    }

    /** 物态的稳定键，如 {@code gas}。 */
    public String stateKey() {
        return state.key();
    }

    /** 是否为独立（无材料）流体。 */
    public boolean standalone() {
        return materialKey.isEmpty();
    }

    /** 关联材料的完整键（仅材料关联流体存在）。 */
    public Optional<String> material() {
        return materialKey.isEmpty() ? Optional.empty() : Optional.of(materialKey);
    }
}
