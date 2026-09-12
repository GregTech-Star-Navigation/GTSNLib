package com.gtsn.lib.ui.theme;

import java.util.Optional;

/**
 * 主题纹理角色：主题可声明纹理 / 图集引用（{@link ThemeId} 形式），控件在支持纹理时优先使用；
 * 主题未提供或纹理资源缺失时回退到纯色角色（可绘制性由渲染后端判定）。
 */
public enum ThemeTextureRole {

    PANEL("panel"),
    BUTTON("button"),
    BUTTON_HOVERED("button_hovered"),
    BUTTON_PRESSED("button_pressed"),
    SLOT("slot"),
    SCROLL_TRACK("scroll_track"),
    SCROLL_THUMB("scroll_thumb"),
    PROGRESS_TRACK("progress_track"),
    PROGRESS_FILL("progress_fill"),
    TOOLTIP("tooltip");

    private final String key;

    ThemeTextureRole(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static Optional<ThemeTextureRole> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        for (ThemeTextureRole role : values()) {
            if (role.key.equals(key)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }
}
