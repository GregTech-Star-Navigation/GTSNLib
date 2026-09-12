package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeColor;
import com.gtsn.lib.ui.theme.ThemeColorRole;

import java.util.function.DoubleFunction;

/**
 * 能量条控件：GT 机器储能（stored / capacity）的水平比例条。
 *
 * <p>状态逻辑（存量钳制到容量、比例映射）与渲染解耦，可在无游戏环境测试。颜色默认取主题
 * {@code PROGRESS_*} / {@code BORDER} 角色，随主题切换；{@link #colors} 可字面量覆盖。
 * 默认绘制内置标签 {@code stored/capacity EU}，可用 {@link #label} 自定义或 {@link #showLabel} 关闭。</p>
 */
public final class EnergyBarWidget extends AbstractWidget {

    private long stored;
    private long capacity;

    private ThemeColor background = ThemeColor.role(ThemeColorRole.PROGRESS_TRACK);
    private ThemeColor fillColor = ThemeColor.role(ThemeColorRole.PROGRESS_FILL);
    private ThemeColor fillTopColor = ThemeColor.role(ThemeColorRole.PROGRESS_FILL_TOP);
    private ThemeColor borderColor = ThemeColor.role(ThemeColorRole.BORDER);
    private ThemeColor labelColor = ThemeColor.role(ThemeColorRole.TEXT_ON_ACCENT);
    private int borderWidth = 1;
    private boolean gradient;
    private boolean showLabel = true;
    private DoubleFunction<String> label;

    public EnergyBarWidget() {
        node().contentMeasurer((widthSpec, heightSpec) ->
                Size.of(widthSpec.resolve(80), heightSpec.resolve(12)));
    }

    /** 设置存量与容量；负值归零，存量不超过容量。 */
    public EnergyBarWidget energy(long stored, long capacity) {
        this.capacity = Math.max(0L, capacity);
        long normalized = Math.max(0L, stored);
        this.stored = capacity > 0L ? Math.min(normalized, this.capacity) : 0L;
        return this;
    }

    public long stored() {
        return stored;
    }

    public long capacity() {
        return capacity;
    }

    /** 能量填充比例 [0, 1]；容量为 0 时返回 0。 */
    public double ratio() {
        if (capacity <= 0L) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) stored / (double) capacity));
    }

    public boolean hasEnergy() {
        return capacity > 0L;
    }

    /** 内置标签文本（{@code stored/capacity EU}）。 */
    public String labelText() {
        return stored + "/" + capacity + " EU";
    }

    /** 字面量覆盖（不随主题变化）；渐变顶色按填充色自动提亮。 */
    public EnergyBarWidget colors(int background, int fillColor, int fillTopColor, int borderColor, int labelColor) {
        this.background = ThemeColor.literal(background);
        this.fillColor = ThemeColor.literal(fillColor);
        this.fillTopColor = ThemeColor.literal(fillTopColor);
        this.borderColor = ThemeColor.literal(borderColor);
        this.labelColor = ThemeColor.literal(labelColor);
        return this;
    }

    public EnergyBarWidget borderWidth(int pixels) {
        if (pixels < 0) {
            throw new IllegalArgumentException("border width must be non-negative: " + pixels);
        }
        this.borderWidth = pixels;
        return this;
    }

    public EnergyBarWidget gradient(boolean gradient) {
        this.gradient = gradient;
        return this;
    }

    /** 关闭 / 打开内置标签。 */
    public EnergyBarWidget showLabel(boolean showLabel) {
        this.showLabel = showLabel;
        return this;
    }

    /** 自定义标签格式化器：入参为归一化比例 [0, 1]。 */
    public EnergyBarWidget label(DoubleFunction<String> formatter) {
        this.label = formatter;
        return this;
    }

    public EnergyBarWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public EnergyBarWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public EnergyBarWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public EnergyBarWidget weight(float weight) {
        node().params().weight(weight);
        return this;
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        if (box.isEmpty()) {
            return;
        }
        Theme theme = context.theme();
        context.fill(box.x(), box.y(), box.width(), box.height(), background.resolve(theme));
        int thickness = Math.min(borderWidth, Math.min(box.width(), box.height()));
        int border = borderColor.resolve(theme);
        if (thickness > 0 && (border >>> 24) != 0) {
            context.fill(box.x(), box.y(), box.width(), thickness, border);
            context.fill(box.x(), box.bottom() - thickness, box.width(), thickness, border);
            context.fill(box.x(), box.y() + thickness, thickness, box.height() - 2 * thickness, border);
            context.fill(box.right() - thickness, box.y() + thickness, thickness,
                    box.height() - 2 * thickness, border);
        }
        Rect inner = box.inset(Insets.all(thickness));
        int fillWidth = (int) Math.round(ratio() * inner.width());
        if (fillWidth > 0 && inner.height() > 0) {
            if (gradient) {
                context.fillGradient(inner.x(), inner.y(), fillWidth, inner.height(),
                        fillTopColor.resolve(theme), fillColor.resolve(theme));
            } else {
                context.fill(inner.x(), inner.y(), fillWidth, inner.height(), fillColor.resolve(theme));
            }
        }
        String text = label != null ? label.apply(ratio()) : labelText();
        if (showLabel && text != null && !text.isEmpty()) {
            int textY = box.y() + Math.max(0, (box.height() - context.textLineHeight()) / 2);
            context.centeredText(text, box.x() + box.width() / 2, textY, labelColor.resolve(theme), false);
        }
    }
}
