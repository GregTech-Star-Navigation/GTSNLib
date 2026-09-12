package com.gtsn.lib.gt.adapter;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * GTCEu 适配层使用的纯映射逻辑：id/name 归一化、资源位置构造与 tag prefix 映射。
 *
 * <p>本类不依赖任何 Minecraft / Forge / GTCEu 类型，可在无游戏进程下直接单测。</p>
 *
 * <p>{@link #toSnakeCase(String)} 移植自 GTCEu 7.5.3
 * {@code com.gregtechceu.gtceu.utils.FormattingUtil#toLowerCaseUnderscore(String)}，
 * 以保证 {@code tagPrefixKey(prefix.name())} 与 {@code TagPrefix#getLowerCaseName()} 一致。
 * GTSNLib 与 GTCEu 同为 LGPL-3.0（ADR-0006），移植合法。</p>
 */
public final class GtNames {

    /** 资源位置命名空间合法字符集（对齐 {@code ResourceLocation}）。 */
    private static final Pattern RESOURCE_NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    /** 资源位置路径合法字符集（对齐 {@code ResourceLocation}）。 */
    private static final Pattern RESOURCE_PATH = Pattern.compile("[a-z0-9/._-]+");

    private GtNames() {
    }

    /**
     * 归一化为 {@code [a-z0-9_]} 形式的 id：去空白、转小写、非字母数字折叠为单个下划线并去除首尾下划线。
     *
     * <pre>
     *   "  Iron Ingot " -&gt; "iron_ingot"
     *   "Red-Alloy"     -&gt; "red_alloy"
     *   "AE2"           -&gt; "ae2"
     *   "!@#"           -&gt; ""
     * </pre>
     */
    public static String normalizeName(String raw) {
        Objects.requireNonNull(raw, "raw");
        String lower = raw.trim().toLowerCase(Locale.ROOT);
        StringBuilder out = new StringBuilder(lower.length());
        boolean lastWasUnderscore = false;
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            boolean alphanumeric = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
            if (alphanumeric) {
                out.append(c);
                lastWasUnderscore = false;
            } else if (!lastWasUnderscore && out.length() > 0) {
                out.append('_');
                lastWasUnderscore = true;
            }
        }
        int end = out.length();
        while (end > 0 && out.charAt(end - 1) == '_') {
            end--;
        }
        return out.substring(0, end);
    }

    /**
     * 将驼峰名转换为 GTCEu 下划线键，行为对齐 {@code FormattingUtil#toLowerCaseUnderscore}。
     *
     * <pre>
     *   "ingot"       -&gt; "ingot"
     *   "tinyDust"    -&gt; "tiny_dust"
     *   "rawOre"      -&gt; "raw_ore"
     *   "wireGtSingle" -&gt; "wire_gt_single"
     * </pre>
     */
    public static String toSnakeCase(String raw) {
        Objects.requireNonNull(raw, "raw");
        StringBuilder result = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char cur = raw.charAt(i);
            result.append(Character.toLowerCase(cur));
            if (i == raw.length() - 1) {
                break;
            }
            char next = raw.charAt(i + 1);
            if (cur == '_' || next == '_') {
                continue;
            }
            boolean nextIsUpper = Character.isUpperCase(next);
            if (Character.isUpperCase(cur) && nextIsUpper) {
                continue;
            }
            if (nextIsUpper || (Character.isDigit(cur) ^ Character.isDigit(next))) {
                result.append('_');
            }
        }
        return result.toString();
    }

    /**
     * tag prefix 的规范键：{@code prefix.name()} 的下划线形式，可用于跨大小写/驼峰查询。
     *
     * @throws IllegalArgumentException 归一化后为空
     */
    public static String tagPrefixKey(String prefixName) {
        Objects.requireNonNull(prefixName, "prefixName");
        String key = toSnakeCase(prefixName.trim());
        if (key.isEmpty()) {
            throw new IllegalArgumentException("prefixName must not be blank");
        }
        return key;
    }

    /**
     * 构造资源位置字符串 {@code namespace:path}，命名空间与路径均须符合 {@code ResourceLocation} 规则。
     *
     * @throws IllegalArgumentException 命名空间或路径非法
     */
    public static String resourceLocation(String namespace, String path) {
        if (namespace == null || !RESOURCE_NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("invalid resource namespace: " + namespace);
        }
        if (path == null || !RESOURCE_PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("invalid resource path: " + path);
        }
        return namespace + ":" + path;
    }

    /**
     * 由命名空间与材料名构造规范资源位置，两部分先分别归一化。
     *
     * @throws IllegalArgumentException 归一化后任一部分为空
     */
    public static String materialId(String namespace, String materialName) {
        String normalizedNamespace = normalizeName(namespace);
        String normalizedPath = normalizeName(materialName);
        if (normalizedNamespace.isEmpty() || normalizedPath.isEmpty()) {
            throw new IllegalArgumentException("namespace and materialName must normalize to non-empty");
        }
        return resourceLocation(normalizedNamespace, normalizedPath);
    }

    /**
     * 按 GTCEu 默认 id 约定（{@code %s_<prefixLower>}）派生材料件 id，例如
     * {@code derivedItemId("tinyDust", "Iron") -&gt; "iron_tiny_dust"}。
     *
     * @throws IllegalArgumentException 归一化后任一部分为空
     */
    public static String derivedItemId(String tagPrefixKey, String materialName) {
        String prefix = tagPrefixKey(tagPrefixKey);
        String material = normalizeName(materialName);
        if (material.isEmpty()) {
            throw new IllegalArgumentException("materialName must normalize to non-empty");
        }
        return material + "_" + prefix;
    }
}
