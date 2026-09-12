package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.input.Keys;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;

import java.util.Objects;

/**
 * 按钮控件：鼠标点击（按下与释放在同一边界内）或在聚焦时按 Enter/Space 触发回调。
 * 具备悬停/按下/聚焦三态视觉。
 */
public final class ButtonWidget extends AbstractWidget {

    private final TextMetrics metrics;
    private final Runnable onClick;
    private String label;

    private int idleFill = 0xFF3B3B3B;
    private int hoverFill = 0xFF4C4C4C;
    private int pressedFill = 0xFF2B2B2B;
    private int borderColor = 0xFF8A8A8A;
    private int focusBorderColor = 0xFF7FB8FF;
    private int textColor = 0xFFF0F0F0;

    private boolean hovered;
    private boolean pressed;
    private boolean focused;
    private boolean enabled = true;

    public ButtonWidget(String label, TextMetrics metrics, Runnable onClick) {
        this.label = Objects.requireNonNull(label, "label");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.onClick = Objects.requireNonNull(onClick, "onClick");
    }

    public ButtonWidget label(String label) {
        this.label = Objects.requireNonNull(label, "label");
        return this;
    }

    public String label() {
        return label;
    }

    public ButtonWidget enabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ButtonWidget colors(int idleFill, int hoverFill, int pressedFill, int textColor) {
        this.idleFill = idleFill;
        this.hoverFill = hoverFill;
        this.pressedFill = pressedFill;
        this.textColor = textColor;
        return this;
    }

    public ButtonWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public ButtonWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public ButtonWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public ButtonWidget padding(Insets padding) {
        node().params().padding(padding);
        return this;
    }

    @Override
    public boolean isFocusable() {
        return enabled;
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        int fill = !enabled ? 0xFF2A2A2A : pressed ? pressedFill : hovered ? hoverFill : idleFill;
        context.fill(box.x(), box.y(), box.width(), box.height(), fill);
        int border = enabled ? (focused ? focusBorderColor : borderColor) : 0xFF5A5A5A;
        if ((border >>> 24) != 0 && box.width() > 0 && box.height() > 0) {
            context.fill(box.x(), box.y(), box.width(), 1, border);
            context.fill(box.x(), box.bottom() - 1, box.width(), 1, border);
            context.fill(box.x(), box.y() + 1, 1, box.height() - 2, border);
            context.fill(box.right() - 1, box.y() + 1, 1, box.height() - 2, border);
        }
        int labelY = box.y() + Math.max(0, (box.height() - context.textLineHeight()) / 2);
        context.centeredText(label, box.x() + box.width() / 2, labelY, enabled ? textColor : 0xFF9A9A9A, false);
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
                onClick.run();
            }
            return true;
        }
        if (event instanceof InputEvent.KeyPressed key && focused
                && (key.keyCode() == Keys.ENTER || key.keyCode() == Keys.SPACE)) {
            onClick.run();
            return true;
        }
        return false;
    }

    @Override
    public void onFocusChanged(boolean focused) {
        this.focused = focused;
    }

    @Override
    public void onHoverChanged(boolean hovered) {
        this.hovered = hovered;
    }
}
