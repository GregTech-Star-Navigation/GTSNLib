package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.input.Keys;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeColor;
import com.gtsn.lib.ui.theme.ThemeColorRole;

import java.util.Objects;

/**
 * 按钮控件：鼠标点击（按下与释放在同一边界内）或在聚焦时按 Enter/Space 触发回调。
 * 具备悬停/按下/聚焦三态视觉；颜色默认取主题角色，{@link #colors} 可字面量覆盖。
 */
public final class ButtonWidget extends AbstractWidget {

    private final TextMetrics metrics;
    private final Runnable onClick;
    private String label;

    private ThemeColor idleFill = ThemeColor.role(ThemeColorRole.BUTTON_BACKGROUND);
    private ThemeColor hoverFill = ThemeColor.role(ThemeColorRole.BUTTON_BACKGROUND_HOVERED);
    private ThemeColor pressedFill = ThemeColor.role(ThemeColorRole.BUTTON_BACKGROUND_PRESSED);
    private ThemeColor borderColor = ThemeColor.role(ThemeColorRole.BORDER);
    private ThemeColor focusBorderColor = ThemeColor.role(ThemeColorRole.FOCUS_RING);
    private ThemeColor textColor = ThemeColor.role(ThemeColorRole.TEXT_STRONG);

    private boolean hovered;
    private boolean pressed;
    private boolean focused;
    private boolean enabled = true;

    public ButtonWidget(String label, TextMetrics metrics, Runnable onClick) {
        this.label = Objects.requireNonNull(label, "label");
        this.metrics = Objects.requireNonNull(metrics, "metrics");
        this.onClick = Objects.requireNonNull(onClick, "onClick");
        node().params().padding(Insets.symmetric(3, 6));
        node().contentMeasurer((widthSpec, heightSpec) -> Size.of(
                widthSpec.resolve(this.metrics.width(this.label)),
                heightSpec.resolve(this.metrics.lineHeight())));
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

    public boolean isEnabled() {
        return enabled;
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

    /** 当前交互状态（禁用 > 按下 > 悬停 > 常规）。 */
    public ButtonState state() {
        if (!enabled) {
            return ButtonState.DISABLED;
        }
        if (pressed) {
            return ButtonState.PRESSED;
        }
        if (hovered) {
            return ButtonState.HOVERED;
        }
        return ButtonState.NORMAL;
    }

    /** 字面量覆盖四态颜色（不随主题变化）。 */
    public ButtonWidget colors(int idleFill, int hoverFill, int pressedFill, int textColor) {
        this.idleFill = ThemeColor.literal(idleFill);
        this.hoverFill = ThemeColor.literal(hoverFill);
        this.pressedFill = ThemeColor.literal(pressedFill);
        this.textColor = ThemeColor.literal(textColor);
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
        Theme theme = context.theme();
        int fill = !enabled ? theme.color(ThemeColorRole.BACKGROUND_DISABLED)
                : pressed ? pressedFill.resolve(theme)
                : hovered ? hoverFill.resolve(theme)
                : idleFill.resolve(theme);
        context.fill(box.x(), box.y(), box.width(), box.height(), fill);
        int border = !enabled ? theme.color(ThemeColorRole.BORDER_DISABLED)
                : focused ? focusBorderColor.resolve(theme)
                : borderColor.resolve(theme);
        if ((border >>> 24) != 0 && box.width() > 0 && box.height() > 0) {
            context.fill(box.x(), box.y(), box.width(), 1, border);
            context.fill(box.x(), box.bottom() - 1, box.width(), 1, border);
            context.fill(box.x(), box.y() + 1, 1, box.height() - 2, border);
            context.fill(box.right() - 1, box.y() + 1, 1, box.height() - 2, border);
        }
        int labelY = box.y() + Math.max(0, (box.height() - context.textLineHeight()) / 2);
        context.centeredText(label, box.x() + box.width() / 2, labelY,
                enabled ? textColor.resolve(theme) : theme.color(ThemeColorRole.TEXT_DISABLED), false);
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
