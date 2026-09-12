package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;

import java.util.function.DoubleFunction;

/**
 * 进度条控件：数值驱动的比例填充，支持任意区间、边框、渐变与自定义标签。
 *
 * <p>状态逻辑（区间钳制、比例映射）与渲染解耦，可在无 MC 环境中测试。</p>
 */
public final class ProgressBarWidget extends AbstractWidget {

    private double min;
    private double max = 1.0;
    private double value;

    private int background = 0xFF101010;
    private int fillColor = 0xFF3F9F4F;
    private int borderColor = 0xFF5A5A5A;
    private int labelColor = 0xFFFFFFFF;
    private int borderWidth = 1;
    private boolean gradient;
    private DoubleFunction<String> label;

    public ProgressBarWidget() {
        node().contentMeasurer((widthSpec, heightSpec) ->
                Size.of(widthSpec.resolve(80), heightSpec.resolve(12)));
    }

    /** 设置数值区间（要求有限且 max > min）。 */
    public ProgressBarWidget range(double min, double max) {
        if (!Double.isFinite(min) || !Double.isFinite(max) || max <= min) {
            throw new IllegalArgumentException("invalid range: min=" + min + " max=" + max);
        }
        this.min = min;
        this.max = max;
        this.value = clamp(value);
        return this;
    }

    /** 设置当前值；超出区间自动钳制，非有限值拒绝。 */
    public ProgressBarWidget value(double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException("value must be finite: " + value);
        }
        this.value = clamp(value);
        return this;
    }

    public double value() {
        return value;
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    /** 归一化进度 [0, 1]。 */
    public double progress() {
        return (clamp(value) - min) / (max - min);
    }

    public ProgressBarWidget colors(int background, int fillColor, int borderColor, int labelColor) {
        this.background = background;
        this.fillColor = fillColor;
        this.borderColor = borderColor;
        this.labelColor = labelColor;
        return this;
    }

    public ProgressBarWidget borderWidth(int pixels) {
        if (pixels < 0) {
            throw new IllegalArgumentException("border width must be non-negative: " + pixels);
        }
        this.borderWidth = pixels;
        return this;
    }

    /** 用垂直渐变替代纯色填充。 */
    public ProgressBarWidget gradient(boolean gradient) {
        this.gradient = gradient;
        return this;
    }

    /** 居中标签格式化器：入参为归一化进度 [0, 1]。 */
    public ProgressBarWidget label(DoubleFunction<String> formatter) {
        this.label = formatter;
        return this;
    }

    public ProgressBarWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public ProgressBarWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public ProgressBarWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public ProgressBarWidget weight(float weight) {
        node().params().weight(weight);
        return this;
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        if (box.isEmpty()) {
            return;
        }
        context.fill(box.x(), box.y(), box.width(), box.height(), background);
        int thickness = Math.min(borderWidth, Math.min(box.width(), box.height()));
        if (thickness > 0 && (borderColor >>> 24) != 0) {
            context.fill(box.x(), box.y(), box.width(), thickness, borderColor);
            context.fill(box.x(), box.bottom() - thickness, box.width(), thickness, borderColor);
            context.fill(box.x(), box.y() + thickness, thickness, box.height() - 2 * thickness, borderColor);
            context.fill(box.right() - thickness, box.y() + thickness, thickness,
                    box.height() - 2 * thickness, borderColor);
        }
        Rect inner = box.inset(Insets.all(thickness));
        int fillWidth = (int) Math.round(progress() * inner.width());
        if (fillWidth > 0 && inner.height() > 0) {
            if (gradient) {
                context.fillGradient(inner.x(), inner.y(), fillWidth, inner.height(),
                        lighten(fillColor), fillColor);
            } else {
                context.fill(inner.x(), inner.y(), fillWidth, inner.height(), fillColor);
            }
        }
        if (label != null) {
            String text = label.apply(progress());
            int textY = box.y() + Math.max(0, (box.height() - context.textLineHeight()) / 2);
            context.centeredText(text, box.x() + box.width() / 2, textY, labelColor, false);
        }
    }

    private double clamp(double v) {
        if (v < min) {
            return min;
        }
        return Math.min(v, max);
    }

    /** 提亮一个色阶（渐变顶色）。 */
    private static int lighten(int argb) {
        int a = argb >>> 24;
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 30);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 30);
        int b = Math.min(255, (argb & 0xFF) + 30);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }
}
