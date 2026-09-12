package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;

/**
 * 开关控件：滑动轨道 + 滑块 + 标签，点击/键盘切换选中状态。
 */
public final class ToggleSwitchWidget extends AbstractToggleWidget {

    private int trackWidth = 24;
    private int trackHeight = 12;
    private int knobInset = 2;
    private int gap = 4;
    private int trackOffColor = 0xFF3A3A3A;
    private int trackOnColor = 0xFF3F7F5F;
    private int knobColor = 0xFFE6E6E6;
    private int borderColor = 0xFF8A8A8A;
    private int hoverBorderColor = 0xFFB8C8D8;
    private int textColor = 0xFFE6E6E6;
    private int disabledTextColor = 0xFF8A8A8A;

    public ToggleSwitchWidget(String label, TextMetrics metrics, BooleanConsumer onChange) {
        super(label, metrics, onChange);
        node().contentMeasurer((widthSpec, heightSpec) -> {
            int width = trackWidth + gap + metrics.width(this.label);
            int height = Math.max(trackHeight, metrics.lineHeight());
            return Size.of(widthSpec.resolve(width), heightSpec.resolve(height));
        });
    }

    public ToggleSwitchWidget trackSize(int width, int height) {
        if (width < 8 || height < 6) {
            throw new IllegalArgumentException("track too small: " + width + "x" + height);
        }
        this.trackWidth = width;
        this.trackHeight = height;
        this.knobInset = Math.min(knobInset, height / 2 - 1);
        return this;
    }

    public ToggleSwitchWidget knobInset(int knobInset) {
        if (knobInset < 1) {
            throw new IllegalArgumentException("knob inset must be >= 1: " + knobInset);
        }
        this.knobInset = knobInset;
        return this;
    }

    public ToggleSwitchWidget gap(int gap) {
        if (gap < 0) {
            throw new IllegalArgumentException("gap must be non-negative: " + gap);
        }
        this.gap = gap;
        return this;
    }

    public ToggleSwitchWidget colors(int trackOffColor, int trackOnColor, int knobColor) {
        this.trackOffColor = trackOffColor;
        this.trackOnColor = trackOnColor;
        this.knobColor = knobColor;
        return this;
    }

    public ToggleSwitchWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public ToggleSwitchWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public ToggleSwitchWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public ToggleSwitchWidget weight(float weight) {
        node().params().weight(weight);
        return this;
    }

    /** 滑块位置：0=关、1=开。 */
    public double knobPosition() {
        return checked ? 1.0 : 0.0;
    }

    /** 滑块当前包围盒（渲染与测试共用）。 */
    public Rect knobBounds() {
        Rect box = bounds();
        int trackY = centeredY(box, trackHeight);
        int knobSize = trackHeight - 2 * knobInset;
        int travel = Math.max(0, trackWidth - 2 * knobInset - knobSize);
        int x = box.x() + knobInset + (int) Math.round(travel * knobPosition());
        return Rect.of(x, trackY + knobInset, knobSize, knobSize);
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        if (box.isEmpty()) {
            return;
        }
        int trackY = centeredY(box, trackHeight);
        Rect track = Rect.of(box.x(), trackY, Math.min(trackWidth, box.width()), trackHeight);
        context.fill(track.x(), track.y(), track.width(), track.height(),
                !enabled ? 0xFF2A2A2A : checked ? trackOnColor : trackOffColor);
        drawBorder(context, track, 1, !enabled ? 0xFF555555 : hovered || pressed ? hoverBorderColor : borderColor);
        Rect knob = knobBounds();
        if (knob.width() > 0 && knob.height() > 0) {
            context.fill(knob.x(), knob.y(), knob.width(), knob.height(), enabled ? knobColor : 0xFF6A6A6A);
        }
        int textX = box.x() + track.width() + gap;
        int textY = centeredY(box, context.textLineHeight());
        context.text(label, textX, textY, enabled ? textColor : disabledTextColor, false);
    }
}
