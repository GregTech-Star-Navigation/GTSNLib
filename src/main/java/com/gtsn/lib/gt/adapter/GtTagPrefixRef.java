package com.gtsn.lib.gt.adapter;

import java.util.Objects;

/**
 * GTCEu tag prefix 在库内的稳定只读视图（不暴露任何 GTCEu 类型）。
 *
 * <p>由 {@link GtceBackend} 从 7.5.3 的 {@code TagPrefix} 映射而来；库内其余代码只依赖本记录。</p>
 *
 * @param name             GTCEu 原始前缀名（{@code TagPrefix#name}，如 {@code tinyDust}）
 * @param lowerCaseName    GTCEu 下划线键（{@code TagPrefix#getLowerCaseName()}，如 {@code tiny_dust}）
 * @param unlocalizedName  翻译键（{@code TagPrefix#getUnlocalizedName()}）
 */
public record GtTagPrefixRef(String name, String lowerCaseName, String unlocalizedName) {

    public GtTagPrefixRef {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lowerCaseName, "lowerCaseName");
        Objects.requireNonNull(unlocalizedName, "unlocalizedName");
    }
}
