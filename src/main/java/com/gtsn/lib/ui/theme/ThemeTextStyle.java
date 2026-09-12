package com.gtsn.lib.ui.theme;

import java.util.Objects;

/**
 * 文本度量（已解析值）：默认阴影、行距与字体资源；缺省为不阴影、零行距、原版默认字体
 * （与库现有视觉一致）。
 *
 * <p>字体（#23）：{@link #fontId()} 为渲染该主题文本所用的字体资源 id；未声明主题字体时为
 * {@link FontId#VANILLA}（原版默认字体）。两参数构造函数保持既有语义（原版字体）。</p>
 */
public record ThemeTextStyle(boolean shadow, int lineSpacing, FontId fontId) {

    public static final ThemeTextStyle DEFAULT = new ThemeTextStyle(false, 0, FontId.VANILLA);

    /** 兼容既有调用的两参数形式：字体为原版默认。 */
    public ThemeTextStyle(boolean shadow, int lineSpacing) {
        this(shadow, lineSpacing, FontId.VANILLA);
    }

    public ThemeTextStyle {
        Objects.requireNonNull(fontId, "fontId");
        if (lineSpacing < 0) {
            throw new IllegalArgumentException("line spacing must be non-negative: " + lineSpacing);
        }
    }

    /** 是否使用原版默认字体（未声明自定义字体）。 */
    public boolean vanillaFont() {
        return FontId.VANILLA.equals(fontId);
    }
}
