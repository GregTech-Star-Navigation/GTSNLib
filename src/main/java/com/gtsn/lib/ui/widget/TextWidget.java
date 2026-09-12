package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeColor;
import com.gtsn.lib.ui.theme.ThemeColorRole;

import java.util.List;
import java.util.Objects;

/**
 * 文本控件：内容宽度/行高来自 {@link TextMetrics}，作为固有尺寸参与布局；可换色与阴影。
 *
 * <p>颜色默认取主题 {@link ThemeColorRole#TEXT} 角色（渲染时解析，切换主题即变色）；
 * 阴影默认取主题文本度量，{@link #shadow(boolean)} 显式覆盖。支持多行文本：
 * {@link #wrap(int)} 按像素宽度自动换行（超长单词硬断），{@link #lineSpacing(int)} 增加行距，
 * {@link #align(TextAlign)} 控制行在包围盒内的水平对齐。</p>
 */
public final class TextWidget extends AbstractWidget {

    private final TextMetrics metrics;
    private String text;
    private ThemeColor color = ThemeColor.role(ThemeColorRole.TEXT);
    private boolean shadow;
    private boolean shadowSet;
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

    /** 显式颜色字面量（覆盖主题角色）。 */
    public TextWidget color(int argb) {
        this.color = ThemeColor.literal(argb);
        return this;
    }

    /** 语义颜色角色（渲染时按当前主题解析）。 */
    public TextWidget colorRole(ThemeColorRole role) {
        this.color = ThemeColor.role(Objects.requireNonNull(role, "role"));
        return this;
    }

    /** 当前配置色（未显式指定时取角色内置默认值）。 */
    public int color() {
        return color.defaultArgb();
    }

    /** 显式阴影开关（覆盖主题文本度量）。 */
    public TextWidget shadow(boolean shadow) {
        this.shadow = shadow;
        this.shadowSet = true;
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
        Theme theme = context.theme();
        int renderedColor = color.resolve(theme);
        boolean renderedShadow = shadowSet ? shadow : theme.textStyle().shadow();
        int lineHeight = context.textLineHeight();
        int totalHeight = lines.size() * lineHeight + Math.max(0, lines.size() - 1) * lineSpacing;
        int y = box.y() + Math.max(0, (box.height() - totalHeight) / 2);
        for (String line : lines) {
            int x = switch (align) {
                case LEFT -> box.x();
                case CENTER -> box.x() + (box.width() - context.textWidth(line)) / 2;
                case RIGHT -> box.right() - context.textWidth(line);
            };
            context.text(line, x, y, renderedColor, renderedShadow);
            y += lineHeight + lineSpacing;
        }
    }
}
