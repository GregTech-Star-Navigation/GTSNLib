package com.gtsn.lib.ui.theme;

import java.util.Objects;

/**
 * 主题圆角刻度（像素）；缺省为 0（直角，与库现有视觉一致）。
 *
 * <p>圆角为形状度量：支持形状渲染的控件读取本值；当前过程化控件（纯矩形填充）
 * 尚未消费，属于预留的扩展点。</p>
 */
public record ThemeRounding(int small, int medium, int large) {

    public static final ThemeRounding DEFAULT = new ThemeRounding(0, 0, 0);

    public ThemeRounding {
        if (small < 0 || medium < 0 || large < 0) {
            throw new IllegalArgumentException("rounding must be non-negative");
        }
    }

    public int pixels(RoundingSize size) {
        return switch (Objects.requireNonNull(size, "size")) {
            case SMALL -> small;
            case MEDIUM -> medium;
            case LARGE -> large;
        };
    }
}
