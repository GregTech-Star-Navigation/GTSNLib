package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeColor;
import com.gtsn.lib.ui.theme.ThemeColorRole;

/**
 * 流体罐控件：GT 机器流体罐存量（amount / capacity）的垂直比例罐。
 *
 * <p>状态逻辑（存量钳制、比例映射）与渲染解耦，可在无游戏环境测试。控件本身不持有
 * {@code FluidStack}——由适配层 / 界面把流体栈翻译为「存量 + 容量 + 名称」，从而保持组件
 * 与 GT / MC 类型解耦。填充自底向上，颜色默认取 {@code PROGRESS_*} / {@code BORDER} 主题角色。</p>
 */
public final class TankWidget extends AbstractWidget {

    private long amount;
    private long capacity;
    private String fluidName = "";

    private ThemeColor background = ThemeColor.role(ThemeColorRole.PROGRESS_TRACK);
    private ThemeColor fillColor = ThemeColor.role(ThemeColorRole.PROGRESS_FILL);
    private ThemeColor borderColor = ThemeColor.role(ThemeColorRole.BORDER);
    private int borderWidth = 1;

    public TankWidget() {
        node().contentMeasurer((widthSpec, heightSpec) ->
                Size.of(widthSpec.resolve(18), heightSpec.resolve(48)));
    }

    /** 设置存量与容量；负值归零，存量不超过容量。 */
    public TankWidget tank(long amount, long capacity) {
        this.capacity = Math.max(0L, capacity);
        long normalized = Math.max(0L, amount);
        this.amount = capacity > 0L ? Math.min(normalized, this.capacity) : 0L;
        return this;
    }

    /** 显示名（可为空字符串）。 */
    public TankWidget fluidName(String fluidName) {
        this.fluidName = fluidName == null ? "" : fluidName;
        return this;
    }

    public long amount() {
        return amount;
    }

    public long capacity() {
        return capacity;
    }

    public String fluidName() {
        return fluidName;
    }

    /** 填充比例 [0, 1]；容量为 0 时返回 0。 */
    public double ratio() {
        if (capacity <= 0L) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) amount / (double) capacity));
    }

    public boolean isEmpty() {
        return amount <= 0L;
    }

    /** 字面量覆盖（不随主题变化）。 */
    public TankWidget colors(int background, int fillColor, int borderColor) {
        this.background = ThemeColor.literal(background);
        this.fillColor = ThemeColor.literal(fillColor);
        this.borderColor = ThemeColor.literal(borderColor);
        return this;
    }

    public TankWidget borderWidth(int pixels) {
        if (pixels < 0) {
            throw new IllegalArgumentException("border width must be non-negative: " + pixels);
        }
        this.borderWidth = pixels;
        return this;
    }

    public TankWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public TankWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public TankWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public TankWidget weight(float weight) {
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
        int fillHeight = (int) Math.round(ratio() * inner.height());
        if (fillHeight > 0 && inner.width() > 0) {
            context.fill(inner.x(), inner.bottom() - fillHeight, inner.width(), fillHeight,
                    fillColor.resolve(theme));
        }
    }
}
