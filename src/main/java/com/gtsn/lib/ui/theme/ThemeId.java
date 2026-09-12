package com.gtsn.lib.ui.theme;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 主题标识（命名空间 + 路径），与 Minecraft {@code ResourceLocation} 同形但无 MC 依赖，
 * 便于主题解析 / 注册表在无客户端环境中测试；客户端加载层再与 {@code ResourceLocation} 互转。
 *
 * <pre>{@code gtsnlib:default   gtsnlib:textures/gui/panel.png}</pre>
 */
public record ThemeId(String namespace, String path) {

    /** 未显式给出命名空间时的缺省值（库自身）。 */
    public static final String DEFAULT_NAMESPACE = "gtsnlib";

    private static final Pattern VALID_NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern VALID_PATH = Pattern.compile("[a-z0-9/._-]+");

    public ThemeId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!VALID_NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("invalid theme namespace: " + namespace);
        }
        if (!VALID_PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("invalid theme path: " + path);
        }
    }

    public static ThemeId of(String namespace, String path) {
        return new ThemeId(namespace, path);
    }

    /**
     * 解析 {@code namespace:path}；缺省命名空间为 {@link #DEFAULT_NAMESPACE}。
     *
     * @throws IllegalArgumentException 非法或空标识
     */
    public static ThemeId parse(String location) {
        if (location == null || location.isBlank()) {
            throw new IllegalArgumentException("theme id must not be blank");
        }
        int separator = location.indexOf(':');
        if (separator < 0) {
            return new ThemeId(DEFAULT_NAMESPACE, location);
        }
        String namespace = location.substring(0, separator);
        String path = location.substring(separator + 1);
        if (namespace.isBlank() || path.isBlank()) {
            throw new IllegalArgumentException("invalid theme id: " + location);
        }
        return new ThemeId(namespace, path);
    }

    public String location() {
        return namespace + ":" + path;
    }

    @Override
    public String toString() {
        return location();
    }
}
