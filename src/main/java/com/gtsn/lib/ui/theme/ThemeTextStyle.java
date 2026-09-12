package com.gtsn.lib.ui.theme;

/**
 * 文本度量（已解析值）：默认阴影与行距；缺省为不阴影、零行距（与库现有视觉一致）。
 */
public record ThemeTextStyle(boolean shadow, int lineSpacing) {

    public static final ThemeTextStyle DEFAULT = new ThemeTextStyle(false, 0);

    public ThemeTextStyle {
        if (lineSpacing < 0) {
            throw new IllegalArgumentException("line spacing must be non-negative: " + lineSpacing);
        }
    }
}
