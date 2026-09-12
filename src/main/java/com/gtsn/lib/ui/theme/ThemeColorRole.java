package com.gtsn.lib.ui.theme;

import java.util.Optional;

/**
 * 主题语义颜色角色：控件不直接写死颜色，而是按角色向当前主题取值；主题 JSON 用
 * {@link #key()}（snake_case）覆盖角色，未覆盖的角色沿主题继承链回退，最终回退到
 * {@link #defaultArgb()}（即库的内置默认视觉）。
 *
 * <p>新增角色属于扩展：枚举追加常量、{@link #defaultArgb()} 给出保底值即可。</p>
 */
public enum ThemeColorRole {

    /** 屏幕背景（alpha 为 0 时由屏幕实现决定是否绘制），默认透明。 */
    BACKGROUND("background", 0x00000000),
    PANEL_BACKGROUND("panel_background", 0xFF181C22),
    PANEL_BORDER("panel_border", 0xFF2E3846),
    PANEL_HEADER("panel_header", 0xFF262C36),
    PANEL_HEADER_RULE("panel_header_rule", 0xFF3C4654),

    TEXT("text", 0xFFE6E6E6),
    TEXT_STRONG("text_strong", 0xFFF0F0F0),
    TEXT_MUTED("text_muted", 0xFF9FB0C0),
    TEXT_DISABLED("text_disabled", 0xFF8A8A8A),
    TEXT_ON_ACCENT("text_on_accent", 0xFFFFFFFF),

    ACCENT("accent", 0xFF7FB8FF),
    SUCCESS("success", 0xFF6FD08A),
    WARNING("warning", 0xFFE0B860),
    DANGER("danger", 0xFFD06060),
    FOCUS_RING("focus_ring", 0xFF7FB8FF),

    BORDER("border", 0xFF8A8A8A),
    BORDER_HOVERED("border_hovered", 0xFFB8C8D8),
    BORDER_STRONG("border_strong", 0xFF9CC4E4),
    BORDER_DISABLED("border_disabled", 0xFF555555),
    BACKGROUND_DISABLED("background_disabled", 0xFF2A2A2A),

    BUTTON_BACKGROUND("button_background", 0xFF3B3B3B),
    BUTTON_BACKGROUND_HOVERED("button_background_hovered", 0xFF4C4C4C),
    BUTTON_BACKGROUND_PRESSED("button_background_pressed", 0xFF2B2B2B),

    CHECKBOX_BACKGROUND("checkbox_background", 0xFF1E1E1E),
    CHECKBOX_DISABLED_BACKGROUND("checkbox_disabled_background", 0xFF242424),
    CHECK_MARK("check_mark", 0xFF6FD08A),

    SWITCH_TRACK_OFF("switch_track_off", 0xFF3A3A3A),
    SWITCH_TRACK_ON("switch_track_on", 0xFF3F7F5F),
    SWITCH_KNOB("switch_knob", 0xFFE6E6E6),
    SWITCH_KNOB_DISABLED("switch_knob_disabled", 0xFF6A6A6A),

    PROGRESS_TRACK("progress_track", 0xFF101010),
    PROGRESS_FILL("progress_fill", 0xFF3F9F4F),
    PROGRESS_FILL_TOP("progress_fill_top", 0xFF5DBD6D),

    SLOT_BACKGROUND("slot_background", 0xFF8B8B8B),
    SLOT_EDGE_DARK("slot_edge_dark", 0xFF373737),
    SLOT_EDGE_LIGHT("slot_edge_light", 0xFFFFFFFF),
    SLOT_SELECTED_BORDER("slot_selected_border", 0xFFFFFFFF),

    OVERLAY_HOVER("overlay_hover", 0x40FFFFFF),
    OVERLAY_PRESSED("overlay_pressed", 0x30000000),
    OVERLAY_DISABLED("overlay_disabled", 0x80000000),

    SCROLL_BACKGROUND("scroll_background", 0xFF141414),
    SCROLL_BORDER("scroll_border", 0xFF3C4654),
    SCROLL_TRACK("scroll_track", 0xFF1E1E1E),
    SCROLL_THUMB("scroll_thumb", 0xFF5A6470),
    SCROLL_THUMB_ACTIVE("scroll_thumb_active", 0xFF8FB0C8),

    DIVIDER("divider", 0xFF4A4A4A),
    BOX_BACKGROUND("box_background", 0xFF202020),
    INPUT_BACKGROUND("input_background", 0xFF1E1E1E),

    TOOLTIP_BACKGROUND("tooltip_background", 0xF0101418),
    TOOLTIP_BORDER("tooltip_border", 0xFF8FB0C8),
    TOOLTIP_TEXT("tooltip_text", 0xFFFFFFFF);

    private final String key;
    private final int defaultArgb;

    ThemeColorRole(String key, int defaultArgb) {
        this.key = key;
        this.defaultArgb = defaultArgb;
    }

    /** JSON 中的键（snake_case）。 */
    public String key() {
        return key;
    }

    /** 内置默认值（无主题覆盖时的最终回退，保持库的历史视觉）。 */
    public int defaultArgb() {
        return defaultArgb;
    }

    public static Optional<ThemeColorRole> fromKey(String key) {
        if (key == null) {
            return Optional.empty();
        }
        for (ThemeColorRole role : values()) {
            if (role.key.equals(key)) {
                return Optional.of(role);
            }
        }
        return Optional.empty();
    }
}
