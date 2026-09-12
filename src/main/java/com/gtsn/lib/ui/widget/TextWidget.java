package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.theme.FontId;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeColor;
import com.gtsn.lib.ui.theme.ThemeColorRole;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 文本控件：内容宽度/行高来自 {@link TextMetrics}，作为固有尺寸参与布局；可换色与阴影。
 *
 * <p>颜色默认取主题 {@link ThemeColorRole#TEXT} 角色（渲染时解析，切换主题即变色）；
 * 阴影默认取主题文本度量，{@link #shadow(boolean)} 显式覆盖。支持多行文本：
 * {@link #wrap(int)} 按像素宽度自动换行（超长单词硬断），{@link #lineSpacing(int)} 增加行距，
 * {@link #align(TextAlign)} 控制行在包围盒内的水平对齐。</p>
 *
 * <p>字体（#23）：默认跟随主题文本字体（{@code ThemeTextStyle.fontId()}，由渲染上下文落实）；
 * {@link #font(FontId)} / {@link #vanillaFont()} 可逐控件覆盖，覆盖同时驱动度量与渲染，
 * 保证换行与固有尺寸按实际字体计算。</p>
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
    private FontId fontOverride;
    private TextMetrics cachedMetrics;
    private FontId cachedMetricsFont;

    public TextWidget(String text, TextMetrics metrics) {
        this.text = Objects.requireNonNull(text, "text");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        node().contentMeasurer((widthSpec, heightSpec) -> {
            List<String> measurementLines = lines();
            TextMetrics measure = effectiveMetrics();
            int width = 0;
            for (String line : measurementLines) {
                width = Math.max(width, measure.width(line));
            }
            int height = measurementLines.size() * measure.lineHeight()
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

    /** 逐控件字体覆盖：度量与渲染均按该字体（覆盖主题字体）。 */
    public TextWidget font(FontId font) {
        this.fontOverride = Objects.requireNonNull(font, "font");
        return this;
    }

    /** 强制原版默认字体（{@link FontId#VANILLA}）：度量与渲染均回退原版。 */
    public TextWidget vanillaFont() {
        return font(FontId.VANILLA);
    }

    /** 取消逐控件覆盖，恢复跟随主题字体（默认）。 */
    public TextWidget useThemeFont() {
        this.fontOverride = null;
        return this;
    }

    /** 当前逐控件字体覆盖；空表示跟随主题。 */
    public Optional<FontId> fontOverride() {
        return Optional.ofNullable(fontOverride);
    }

    /**
     * 实际用于度量 / 换行的字体度量：有覆盖时包一层 {@link TextMetrics#width(String, FontId)}，
     * 其余方法与注入度量一致。
     */
    private TextMetrics effectiveMetrics() {
        if (fontOverride == null) {
            return metrics;
        }
        if (cachedMetrics == null || !fontOverride.equals(cachedMetricsFont)) {
            FontId font = fontOverride;
            cachedMetricsFont = font;
            cachedMetrics = new TextMetrics() {
                @Override
                public int width(String value) {
                    return metrics.width(value, font);
                }

                @Override
                public int lineHeight() {
                    return metrics.lineHeight();
                }
            };
        }
        return cachedMetrics;
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
        return TextWrapper.wrap(text, effectiveMetrics(), wrapWidth);
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
            int lineWidth = fontOverride == null ? context.textWidth(line) : context.textWidth(line, fontOverride);
            int x = switch (align) {
                case LEFT -> box.x();
                case CENTER -> box.x() + (box.width() - lineWidth) / 2;
                case RIGHT -> box.right() - lineWidth;
            };
            if (fontOverride == null) {
                context.text(line, x, y, renderedColor, renderedShadow);
            } else {
                context.text(line, x, y, renderedColor, renderedShadow, fontOverride);
            }
            y += lineHeight + lineSpacing;
        }
    }
}
