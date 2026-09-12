package com.gtsn.lib.ui.widget;

import java.util.List;
import java.util.Objects;

/**
 * 工具提示内容：若干文本行，鼠标悬停时由 {@code WidgetHost} 在控件树之上绘制。
 *
 * <p>纯数据、无 MC 依赖；行列表不可变。</p>
 */
public record Tooltip(List<String> lines) {

    public Tooltip {
        Objects.requireNonNull(lines, "lines");
        lines = List.copyOf(lines);
    }

    public static Tooltip of(String... lines) {
        return new Tooltip(List.of(lines));
    }

    public static Tooltip ofLines(List<String> lines) {
        return new Tooltip(lines);
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }
}
