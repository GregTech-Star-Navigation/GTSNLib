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
 * 开关控件：滑动轨道 + 滑块 + 标签，点击/键盘切换选中状态。
 *
 * <p>颜色默认取主题角色，{@link #colors} 可字面量覆盖。</p>
 */
public final class ToggleSwitchWidget extends AbstractToggleWidget {

    private int trackWidth = 24;
    private int trackHeight = 12;
    private int knobInset = 2;
    private int gap = 4;
    private ThemeColor trackOffColor = ThemeColor.role(ThemeColorRole.SWITCH_TRACK_OFF);
    private ThemeColor trackOnColor = ThemeColor.role(ThemeColorRole.SWITCH_TRACK_ON);
    private ThemeColor knobColor = ThemeColor.role(ThemeColorRole.SWITCH_KNOB);
    private ThemeColor textColor = ThemeColor.role(ThemeColorRole.TEXT);

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

    /** 字面量覆盖轨道 / 滑块颜色（不随主题变化）。 */
    public ToggleSwitchWidget colors(int trackOffColor, int trackOnColor, int knobColor) {
        this.trackOffColor = ThemeColor.literal(trackOffColor);
        this.trackOnColor = ThemeColor.literal(trackOnColor);
        this.knobColor = ThemeColor.literal(knobColor);
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
        Theme theme = context.theme();
        int trackY = centeredY(box, trackHeight);
        Rect track = Rect.of(box.x(), trackY, Math.min(trackWidth, box.width()), trackHeight);
        context.fill(track.x(), track.y(), track.width(), track.height(),
                !enabled ? theme.color(ThemeColorRole.BACKGROUND_DISABLED)
                        : checked ? trackOnColor.resolve(theme)
                        : trackOffColor.resolve(theme));
        int border = !enabled ? theme.color(ThemeColorRole.BORDER_DISABLED)
                : hovered || pressed ? theme.color(ThemeColorRole.BORDER_HOVERED)
                : theme.color(ThemeColorRole.BORDER);
        drawBorder(context, track, 1, border);
        Rect knob = knobBounds();
        if (knob.width() > 0 && knob.height() > 0) {
            context.fill(knob.x(), knob.y(), knob.width(), knob.height(),
                    enabled ? knobColor.resolve(theme) : theme.color(ThemeColorRole.SWITCH_KNOB_DISABLED));
        }
        int textX = box.x() + track.width() + gap;
        int textY = centeredY(box, context.textLineHeight());
        context.text(label, textX, textY,
                enabled ? textColor.resolve(theme) : theme.color(ThemeColorRole.TEXT_DISABLED), false);
    }
}
