package com.gtsn.lib.ui.theme;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 主题解析器：把一组 {@link ThemeDefinition} 解析为 {@link ThemeRegistry}，
 * 处理 {@code extends} 继承链（含环与未知父主题的容错），并保证默认主题始终存在。
 */
public final class ThemeResolver {

    private ThemeResolver() {
    }

    /**
     * 解析定义集合；重复 id 以最后出现者为准。
     *
     * <p>容错规则：未知父主题 → 该主题无父（回退各角色内置默认值）；{@code extends} 成环 →
     * 断开成环边，双方仍可解析。</p>
     */
    public static ThemeRegistry resolve(Collection<ThemeDefinition> definitions) {
        Objects.requireNonNull(definitions, "definitions");
        Map<ThemeId, ThemeDefinition> byId = new LinkedHashMap<>();
        for (ThemeDefinition definition : definitions) {
            Objects.requireNonNull(definition, "definition");
            byId.put(definition.id(), definition); // 后者覆盖：重复 id 以最后为准
        }

        Map<ThemeId, Theme> resolved = new HashMap<>();
        List<Theme> themes = new ArrayList<>();
        for (ThemeId id : byId.keySet()) {
            Theme theme = resolveTheme(id, byId, resolved, new HashSet<>());
            if (theme != null) {
                themes.add(theme);
            }
        }
        return ThemeRegistry.of(themes);
    }

    private static Theme resolveTheme(ThemeId id, Map<ThemeId, ThemeDefinition> definitions,
                                      Map<ThemeId, Theme> cache, Set<ThemeId> visiting) {
        Theme cached = cache.get(id);
        if (cached != null) {
            return cached;
        }
        ThemeDefinition definition = definitions.get(id);
        if (definition == null) {
            return null; // 未知父主题：该边解析为空
        }
        if (!visiting.add(id)) {
            return null; // extends 成环：断开成环边
        }

        Theme parent = null;
        if (definition.parentId().isPresent()) {
            parent = resolveTheme(definition.parentId().orElseThrow(), definitions, cache, visiting);
        }
        visiting.remove(id);

        Theme theme = new Theme(id, definition.name(), definition, parent);
        cache.put(id, theme);
        return theme;
    }
}
