package com.gtsn.lib.gt.adapter;

import java.util.Objects;

/**
 * GTCEu 材料在库内的稳定只读视图（不暴露任何 GTCEu 类型）。
 *
 * <p>由 {@link GtceBackend} 从 7.5.3 的 {@code Material} 映射而来；库内其余代码只依赖本记录。</p>
 *
 * @param name             材料 id 路径（{@code Material.getName()}，已是小写）
 * @param namespace        材料所在命名空间（{@code Material.getModid()}）
 * @param path             资源位置路径（{@code ResourceLocation#getPath()}）
 * @param resourceLocation 完整资源位置字符串，形如 {@code gtceu:iron}
 * @param unlocalizedName  翻译键（{@code Material.getUnlocalizedName()}）
 * @param chemicalFormula  化学式，可能为 {@code null}
 */
public record GtMaterialRef(
        String name,
        String namespace,
        String path,
        String resourceLocation,
        String unlocalizedName,
        String chemicalFormula) {

    public GtMaterialRef {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        Objects.requireNonNull(resourceLocation, "resourceLocation");
        Objects.requireNonNull(unlocalizedName, "unlocalizedName");
    }
}
