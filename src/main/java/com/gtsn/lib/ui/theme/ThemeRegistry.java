package com.gtsn.lib.ui.theme;

/**
 * 主题注册表：id → 已解析主题的只读集合，始终包含内置默认主题作为兜底。
 */
public final class ThemeRegistry {

    /** 内置默认主题 id。 */
    public static final ThemeId DEFAULT_ID = ThemeId.of("gtsnlib", "default");

    private ThemeRegistry() {
    }
}
