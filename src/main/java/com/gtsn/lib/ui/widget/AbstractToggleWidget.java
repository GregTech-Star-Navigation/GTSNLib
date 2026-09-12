package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.input.Keys;
import com.gtsn.lib.ui.layout.Rect;

import java.util.Objects;

/**
 * 开关类控件的行为基类：选中状态、启用/悬停/按下/聚焦状态与统一交互语义
 * （点击在边界内按下并释放切换；聚焦时 Enter/Space 切换；禁用时不响应且不可聚焦）。
 *
 * <p>{@link #checked(boolean)} 为静默设置（用于初始化/外部同步），
 * 用户交互走 {@link #toggle()} 并触发回调。</p>
 */
public abstract class AbstractToggleWidget extends AbstractWidget {

    protected final TextMetrics metrics;
    protected String label;
    protected final BooleanConsumer onChange;

    protected boolean checked;
    protected boolean enabled = true;
    protected boolean hovered;
    protected boolean pressed;
    protected boolean focused;

    protected AbstractToggleWidget(String label, TextMetrics metrics, BooleanConsumer onChange) {
        this.label = Objects.requireNonNull(label, "label");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    public String label() {
        return label;
    }

    public AbstractToggleWidget label(String label) {
        this.label = Objects.requireNonNull(label, "label");
        return this;
    }

    public boolean checked() {
        return checked;
    }

    /** 静默设置选中状态（不触发回调）。 */
    public AbstractToggleWidget checked(boolean checked) {
        this.checked = checked;
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public AbstractToggleWidget enabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            pressed = false;
        }
        return this;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isPressed() {
        return pressed;
    }

    public boolean isFocused() {
        return focused;
    }

    /** 切换选中状态并触发回调（用户交互入口）。 */
    public void toggle() {
        checked = !checked;
        onChange.accept(checked);
    }

    @Override
    public boolean isFocusable() {
        return enabled;
    }

    @Override
    public void onFocusChanged(boolean focused) {
        this.focused = focused;
    }

    @Override
    public void onHoverChanged(boolean hovered) {
        this.hovered = hovered;
    }

    @Override
    public boolean onInput(InputEvent event) {
        if (!enabled) {
            return false;
        }
        if (event instanceof InputEvent.MousePressed press
                && press.button() == 0 && bounds().contains(press.x(), press.y())) {
            pressed = true;
            return true;
        }
        if (event instanceof InputEvent.MouseReleased release && release.button() == 0 && pressed) {
            pressed = false;
            if (bounds().contains(release.x(), release.y())) {
                toggle();
            }
            return true;
        }
        if (event instanceof InputEvent.KeyPressed key && focused
                && (key.keyCode() == Keys.ENTER || key.keyCode() == Keys.SPACE)) {
            toggle();
            return true;
        }
        return false;
    }

    /** 主体内容区的垂直居中 y（用于盒/轨道与文本对齐）。 */
    protected int centeredY(Rect box, int contentHeight) {
        return box.y() + Math.max(0, (box.height() - contentHeight) / 2);
    }

    /** 画一个 1px（或指定厚度）的矩形边框。 */
    protected static void drawBorder(com.gtsn.lib.ui.render.RenderContext context, Rect box,
                                     int thickness, int color) {
        int t = Math.min(thickness, Math.min(box.width(), box.height()));
        if (t <= 0 || (color >>> 24) == 0) {
            return;
        }
        context.fill(box.x(), box.y(), box.width(), t, color);
        context.fill(box.x(), box.bottom() - t, box.width(), t, color);
        context.fill(box.x(), box.y() + t, t, box.height() - 2 * t, color);
        context.fill(box.right() - t, box.y() + t, t, box.height() - 2 * t, color);
    }
}
