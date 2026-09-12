package com.gtsn.lib.ui.theme;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 字体资源标识（命名空间 + 路径），指向 {@code assets/<namespace>/font/<path>.json} 的字体定义，
 * 与 Minecraft {@code ResourceLocation} 同形但无 MC 依赖，便于主题解析 / 文本度量在无客户端环境中测试。
 *
 * <pre>{@code gtsnlib:sarasa_ui_sc   minecraft:default}</pre>
 *
 * <p>{@link #VANILLA} 为原版默认字体哨兵：主题未声明自定义字体、或显式要求原版渲染时使用。</p>
 */
public record FontId(String namespace, String path) {

    /** 未显式给出命名空间时的缺省值（库自身）。 */
    public static final String DEFAULT_NAMESPACE = "gtsnlib";

    private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern VALID_PATH = Pattern.compile("[a-z0-9/._-]+");

    /** 原版默认字体（{@code minecraft:default}）哨兵（声明在校验 Pattern 之后，避免类初始化顺序问题）。 */
    public static final FontId VANILLA = new FontId("minecraft", "default");

    public FontId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!VALID_NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("invalid font namespace: " + namespace);
        }
        if (!VALID_PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("invalid font path: " + path);
        }
    }

    public static FontId of(String namespace, String path) {
        return new FontId(namespace, path);
    }

    /**
     * 解析 {@code namespace:path}；缺省命名空间为 {@link #DEFAULT_NAMESPACE}。
     *
     * @throws IllegalArgumentException 非法或空标识
     */
    public static FontId parse(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("font id must not be blank");
        }
        int separator = location.indexOf(':');
        if (separator < 0) {
            return new FontId(DEFAULT_NAMESPACE, location);
        }
        String namespace = location.substring(0, separator);
        String path = location.substring(separator + 1);
        if (namespace.isBlank() || path.isBlank()) {
            throw new IllegalArgumentException("invalid font id: " + location);
        }
        return new FontId(namespace, path);
    }

    public String location() {
        return namespace + ":" + path;
    }

    @Override
    public String toString() {
        return location();
    }
}
