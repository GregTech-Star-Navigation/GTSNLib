package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;

import java.util.Objects;

/**
 * 文本控件：内容宽度/行高来自 {@link TextMetrics}，作为固有尺寸参与布局；可换色与阴影。
 */
public final class TextWidget extends AbstractWidget {

    private final TextMetrics metrics;
    private String text;
    private int color = 0xFFE6E6E6;
    private boolean shadow;

    public TextWidget(String text, TextMetrics metrics) {
        this.text = Objects.requireNonNull(text, "text");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        node().contentMeasurer((widthSpec, heightSpec) -> Size.of(
                widthSpec.resolve(this.metrics.width(this.text)),
                heightSpec.resolve(this.metrics.lineHeight())));
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

    public TextWidget shadow(boolean shadow) {
        this.shadow = shadow;
        return this;
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

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        int lineHeight = context.textLineHeight();
        int y = box.y() + Math.max(0, (box.height() - lineHeight) / 2);
        context.text(text, box.x(), y, color, shadow);
    }
}
