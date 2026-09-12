package com.gtsn.lib.ui.theme;

import java.util.Objects;

/**
 * 颜色引用：显式的 ARGB 字面量，或一个语义角色。
 *
 * <p>控件字段默认持有角色（渲染时按当前主题解析）；调用方以字面量覆盖后，
 * 该控件不再随主题切换。解析失败不可能发生：角色未在任何主题中定义时回退到
 * {@link ThemeColorRole#defaultArgb()}。</p>
 */
public final class ThemeColor {

    private final Integer literal;
    private final ThemeColorRole role;

    private ThemeColor(Integer literal, ThemeColorRole role) {
        this.literal = literal;
        this.role = role;
    }

    public static ThemeColor literal(int argb) {
        return new ThemeColor(argb, null);
    }

    public static ThemeColor role(ThemeColorRole role) {
        return new ThemeColor(null, Objects.requireNonNull(role, "role"));
    }

    public boolean isLiteral() {
        return literal != null;
    }

    public boolean isRole() {
        return role != null;
    }

    /** 角色引用对应的角色；字面量时为 {@code null}。 */
    public ThemeColorRole role() {
        return role;
    }

    /** 用当前主题解析颜色；{@code theme} 为 {@code null} 时回退到角色内置默认值。 */
    public int resolve(Theme theme) {
        if (literal != null) {
            return literal;
        }
        return theme != null ? theme.color(role) : role.defaultArgb();
    }

    /** 不经主题的保底颜色（字面量或角色内置默认值）；用于 getter 契约兼容。 */
    public int defaultArgb() {
        return literal != null ? literal : role.defaultArgb();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ThemeColor that)) {
            return false;
        }
        return Objects.equals(literal, that.literal) && role == that.role;
    }

    @Override
    public int hashCode() {
        return Objects.hash(literal, role);
    }

    @Override
    public String toString() {
        return literal != null ? String.format("ThemeColor#%08X", literal) : "ThemeColor(" + role.key() + ")";
    }
}
