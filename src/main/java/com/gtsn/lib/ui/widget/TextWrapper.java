package com.gtsn.lib.ui.widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 文本换行算法：按词贪心换行，单词本身超宽时按字符硬断；显式 {@code \n} 强制换行。
 *
 * <p>宽度以 {@link TextMetrics} 度量（真实字体实现下与渲染宽度一致），纯函数、无 MC 依赖。</p>
 */
public final class TextWrapper {

    private TextWrapper() {
    }

    /**
     * 把文本按最大像素宽度换行为行列表。
     *
     * @param text     原文（可含 {@code \n}）
     * @param metrics  字体度量
     * @param maxWidth 像素上限；{@code <= 0} 时不换行，原样返回单行
     */
    public static List<String> wrap(String text, TextMetrics metrics, int maxWidth) {
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(metrics, "metrics");
        if (maxWidth <= 0) {
            return List.of(text);
        }
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                lines.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            int lineWidth = 0;
            for (String word : paragraph.split(" ")) {
                if (word.isEmpty()) {
                    continue;
                }
                while (true) {
                    int separator = line.length() == 0 ? 0 : metrics.width(" ");
                    int candidate = lineWidth + separator + metrics.width(word);
                    if (candidate <= maxWidth || word.length() == 1) {
                        if (line.length() > 0 && separator > 0) {
                            line.append(' ');
                        }
                        line.append(word);
                        lineWidth = candidate;
                        break;
                    }
                    if (line.length() > 0) {
                        lines.add(line.toString());
                        line.setLength(0);
                        lineWidth = 0;
                        continue;
                    }
                    int breakAt = breakIndex(word, metrics, maxWidth);
                    lines.add(word.substring(0, breakAt));
                    word = word.substring(breakAt);
                }
            }
            if (line.length() > 0) {
                lines.add(line.toString());
            }
        }
        return lines;
    }

    /** 单词在不超过 maxWidth 前提下的最大前缀字符数（至少 1 个字符）。 */
    private static int breakIndex(String word, TextMetrics metrics, int maxWidth) {
        int breakAt = 1;
        for (int i = 2; i <= word.length(); i++) {
            if (metrics.width(word.substring(0, i)) > maxWidth) {
                break;
            }
            breakAt = i;
        }
        return breakAt;
    }
}
