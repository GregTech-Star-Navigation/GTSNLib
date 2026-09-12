package com.gtsn.lib.ui.theme;

import java.util.Optional;

/**
 * 已注册主题的解析视图：持有原始 {@link ThemeDefinition} 与父主题链，
 * 取值时沿链回退（子主题 → 父主题 → … → 内置默认值，如 {@link ThemeColorRole#defaultArgb()}）。
 *
 * <p>实例由 {@link ThemeResolver} 构建；内置默认主题（{@link #builtinDefault()}）在
 * 资源不可用或未加载时保底。</p>
 */
public final class Theme {

    private final ThemeId id;
    private final String name;
    private final ThemeDefinition definition;
    private final Theme parent;

    Theme(ThemeId id, String name, ThemeDefinition definition, Theme parent) {
        this.id = id;
        this.name = name;
        this.definition = definition;
        this.parent = parent;
    }

    /** 资源缺失时的内置默认主题：全部角色取枚举默认值（与库历史视觉一致）。 */
    public static Theme builtinDefault() {
        return new Theme(ThemeRegistry.DEFAULT_ID, "默认", null, null);
    }

    public ThemeId id() {
        return id;
    }

    /** 展示名（JSON {@code name}，缺省为 id 路径段）。 */
    public String name() {
        return name;
    }

    public Optional<ThemeDefinition> definition() {
        return Optional.ofNullable(definition);
    }

    /** 已解析的父主题；{@code extends} 缺失或指向未知主题时为空。 */
    public Optional<Theme> parent() {
        return Optional.ofNullable(parent);
    }

    /** 声明的父主题 id（来自 {@code extends}）。 */
    public Optional<ThemeId> parentId() {
        return definition != null ? definition.parentId() : Optional.empty();
    }

    public int color(ThemeColorRole role) {
        if (definition != null) {
            Integer value = definition.colors().get(role);
            if (value != null) {
                return value;
            }
        }
        if (parent != null) {
            return parent.color(role);
        }
        return role.defaultArgb();
    }

    public ThemeTextStyle textStyle() {
        Boolean shadow = inheritedShadow();
        Integer lineSpacing = inheritedLineSpacing();
        return new ThemeTextStyle(
                shadow != null ? shadow : ThemeTextStyle.DEFAULT.shadow(),
                lineSpacing != null ? lineSpacing : ThemeTextStyle.DEFAULT.lineSpacing(),
                inheritedFont());
    }

    public int spacing(Spacing step) {
        Integer declared = inheritedSpacing(step);
        return declared != null ? declared : ThemeSpacing.DEFAULT.pixels(step);
    }

    public int rounding(RoundingSize size) {
        Integer declared = inheritedRounding(size);
        return declared != null ? declared : ThemeRounding.DEFAULT.pixels(size);
    }

    /** 角色对应的主题纹理引用；主题链未声明时为 {@code null}（控件回退纯色）。 */
    public ThemeId texture(ThemeTextureRole role) {
        if (definition != null) {
            ThemeId value = definition.texture(role).orElse(null);
            if (value != null) {
                return value;
            }
        }
        if (parent != null) {
            return parent.texture(role);
        }
        return null;
    }

    private Boolean inheritedShadow() {
        if (definition != null && definition.textShadow().isPresent()) {
            return definition.textShadow().orElseThrow();
        }
        return parent != null ? parent.inheritedShadow() : null;
    }

    private Integer inheritedLineSpacing() {
        if (definition != null && definition.textLineSpacing().isPresent()) {
            return definition.textLineSpacing().orElseThrow();
        }
        return parent != null ? parent.inheritedLineSpacing() : null;
    }

    /** 沿继承链解析文本字体；链上均未声明时为原版默认字体（{@link FontId#VANILLA}）。 */
    private FontId inheritedFont() {
        if (definition != null && definition.textFont().isPresent()) {
            return definition.textFont().orElseThrow();
        }
        return parent != null ? parent.inheritedFont() : FontId.VANILLA;
    }

    private Integer inheritedSpacing(Spacing step) {
        if (definition != null && definition.spacing(step).isPresent()) {
            return definition.spacing(step).orElseThrow();
        }
        return parent != null ? parent.inheritedSpacing(step) : null;
    }

    private Integer inheritedRounding(RoundingSize size) {
        if (definition != null && definition.rounding(size).isPresent()) {
            return definition.rounding(size).orElseThrow();
        }
        return parent != null ? parent.inheritedRounding(size) : null;
    }
}
