package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;

import java.util.List;
import java.util.Objects;

/**
 * 文本控件：内容宽度/行高来自 {@link TextMetrics}，作为固有尺寸参与布局；可换色与阴影。
 *
 * <p>支持多行文本：{@link #wrap(int)} 按像素宽度自动换行（超长单词硬断），
 * {@link #lineSpacing(int)} 增加行距，{@link #align(TextAlign)} 控制行在包围盒内的水平对齐。</p>
 */
public final class TextWidget extends AbstractWidget {

    private final TextMetrics metrics;
    private String text;
    private int color = 0xFFE6E6E6;
    private boolean shadow;
    private TextAlign align = TextAlign.LEFT;
    private int wrapWidth;
    private int lineSpacing;

    public TextWidget(String text, TextMetrics metrics) {
        this.text = Objects.requireNonNull(text, "text");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        node().contentMeasurer((widthSpec, heightSpec) -> {
            List<String> measurementLines = lines();
            int width = 0;
            for (String line : measurementLines) {
                width = Math.max(width, metrics.width(line));
            }
            int height = measurementLines.size() * metrics.lineHeight()
                    + Math.max(0, measurementLines.size() - 1) * lineSpacing;
            return Size.of(widthSpec.resolve(width), heightSpec.resolve(height));
        });
    }

    public TextWidget text(String text) {
        this.text = Objects.requireNonNull(text, "text");
        return this;
    }

    public String text() {
        return text;
    }

    public TextWidget color(int argb) {
        this.color = argb;
        return this;
    }

    public int color() {
        return color;
    }

    public TextWidget shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
    }

    public TextWidget align(TextAlign align) {
        this.align = Objects.requireNonNull(align, "align");
        return this;
    }

    public TextAlign align() {
        return align;
    }

    /** 启用自动换行，行为像素宽度上限；{@code 0} 关闭（默认）。 */
    public TextWidget wrap(int maxWidth) {
        if (maxWidth < 0) {
            throw new IllegalArgumentException("wrap width must be non-negative: " + maxWidth);
        }
        this.wrapWidth = maxWidth;
        return this;
    }

    public int wrapWidth() {
        return wrapWidth;
    }

    /** 行间额外像素间距；{@code 0} 表示紧凑行（默认）。 */
    public TextWidget lineSpacing(int lineSpacing) {
        if (lineSpacing < 0) {
            throw new IllegalArgumentException("line spacing must be non-negative: " + lineSpacing);
        }
        this.lineSpacing = lineSpacing;
        return this;
    }

    public int lineSpacing() {
        return lineSpacing;
    }

    public TextWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public TextWidget padding(Insets padding) {
        node().params().padding(padding);
        return this;
    }

    public TextWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    /** 当前换行结果；未启用换行时为单行。 */
    public List<String> lines() {
        return TextWrapper.wrap(text, metrics, wrapWidth);
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        List<String> lines = lines();
        int lineHeight = context.textLineHeight();
        int totalHeight = lines.size() * lineHeight + Math.max(0, lines.size() - 1) * lineSpacing;
        int y = box.y() + Math.max(0, (box.height() - totalHeight) / 2);
        for (String line : lines) {
            int x = switch (align) {
                case LEFT -> box.x();
                case CENTER -> box.x() + (box.width() - context.textWidth(line)) / 2;
                case RIGHT -> box.right() - context.textWidth(line);
            };
            context.text(line, x, y, color, shadow);
            y += lineHeight + lineSpacing;
        }
    }
}
