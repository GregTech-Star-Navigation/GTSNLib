package com.gtsn.lib.ui.theme;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 主题注册表：id → 已解析主题的只读集合，始终包含默认主题作为兜底
 * （{@link ThemeRegistry#DEFAULT_ID}；资源未提供时使用内置默认实例）。
 */
public final class ThemeRegistry {

    /** 默认主题 id（库内置；资源可提供同名主题覆盖其视觉）。 */
    public static final ThemeId DEFAULT_ID = ThemeId.of("gtsnlib", "default");

    private static final ThemeRegistry BUILTIN = new ThemeRegistry(List.of());

    private final Theme defaultTheme;
    private final Map<ThemeId, Theme> themes;
    private final List<ThemeId> ids;

    private ThemeRegistry(List<Theme> themes) {
        Theme explicitDefault = null;
        List<Theme> others = new ArrayList<>();
        for (Theme theme : themes) {
            if (DEFAULT_ID.equals(theme.id())) {
                explicitDefault = theme;
            } else {
                others.add(theme);
            }
        }
        this.defaultTheme = explicitDefault != null ? explicitDefault : Theme.builtinDefault();
        others.sort(Comparator.comparing(theme -> theme.id().location()));

        Map<ThemeId, Theme> ordered = new LinkedHashMap<>();
        ordered.put(DEFAULT_ID, defaultTheme);
        for (Theme theme : others) {
            ordered.putIfAbsent(theme.id(), theme);
        }
        this.themes = Collections.unmodifiableMap(ordered);
        this.ids = List.copyOf(ordered.keySet());
    }

    /** 由解析器构建；{@code themes} 中可含默认主题实例。 */
    static ThemeRegistry of(List<Theme> themes) {
        return new ThemeRegistry(themes);
    }

    /** 仅含内置默认主题的注册表（资源缺失 / 未加载时的保底）。 */
    public static ThemeRegistry builtin() {
        return BUILTIN;
    }

    /** 兜底主题；永不失败。 */
    public Theme defaultTheme() {
        return defaultTheme;
    }

    public Optional<Theme> find(ThemeId id) {
        return Optional.ofNullable(themes.get(Objects.requireNonNull(id, "id")));
    }

    /** 指定 id 的主题；未知 id 回退到默认主题。 */
    public Theme get(ThemeId id) {
        return themes.getOrDefault(Objects.requireNonNull(id, "id"), defaultTheme);
    }

    /** 注册顺序：默认主题在前，其余按 id 字典序（保证 {@code cycle} 确定性）。 */
    public List<ThemeId> ids() {
        return ids;
    }

    public int size() {
        return themes.size();
    }
}
