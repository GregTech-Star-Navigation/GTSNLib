package com.gtsn.lib.ui.demo;

import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.input.Keys;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.theme.ThemeColorRole;
import com.gtsn.lib.ui.widget.AbstractWidget;

import java.util.Objects;

/**
 * 键盘探针控件：可聚焦，展示最近按键与字符输入（内核输入模型的演示部件，非通用组件）。
 * 颜色取主题角色（输入框背景 / 边框 / 聚焦环 / 强文本）。
 */
public final class KeypadWidget extends AbstractWidget {

    private final DemoState state;
    private boolean focused;

    public KeypadWidget(DemoState state) {
        this.state = Objects.requireNonNull(state, "state");
        node().params().size(Sizing.fill(), Sizing.fixed(22)).padding(Insets.symmetric(3, 5));
    }

    @Override
    public boolean isFocusable() {
        return true;
    }

    @Override
    public void onFocusChanged(boolean focused) {
        this.focused = focused;
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        context.fill(box.x(), box.y(), box.width(), box.height(),
                context.theme().color(ThemeColorRole.INPUT_BACKGROUND));
        int border = focused
                ? context.theme().color(ThemeColorRole.FOCUS_RING)
                : context.theme().color(ThemeColorRole.BORDER);
        if (box.width() > 0 && box.height() > 0) {
            context.fill(box.x(), box.y(), box.width(), 1, border);
            context.fill(box.x(), box.bottom() - 1, box.width(), 1, border);
            context.fill(box.x(), box.y() + 1, 1, box.height() - 2, border);
            context.fill(box.right() - 1, box.y() + 1, 1, box.height() - 2, border);
        }
        String hint = focused
                ? "输入: " + (state.typed().isEmpty() ? "(请敲键盘)" : state.typed())
                : "点击聚焦后按键";
        String line = "按键: " + state.lastKey() + "  |  " + hint;
        context.text(line, box.x() + 5,
                box.y() + Math.max(0, (box.height() - context.textLineHeight()) / 2),
                context.theme().color(ThemeColorRole.TEXT_STRONG), false);
    }

    @Override
    public boolean onInput(InputEvent event) {
        if (event instanceof InputEvent.KeyPressed key) {
            state.lastKey(describe(key.keyCode()));
            return true;
        }
        if (event instanceof InputEvent.CharTyped typed) {
            state.appendTyped(typed.codePoint());
            return true;
        }
        return false;
    }

    private static String describe(int keyCode) {
        if (keyCode >= 'A' && keyCode <= 'Z' || keyCode >= '0' && keyCode <= '9') {
            return String.valueOf((char) keyCode);
        }
        return switch (keyCode) {
            case Keys.ENTER -> "Enter";
            case Keys.SPACE -> "Space";
            case Keys.ESCAPE -> "Esc";
            case Keys.TAB -> "Tab";
            case Keys.BACKSPACE -> "Backspace";
            case Keys.DELETE -> "Delete";
            case Keys.LEFT -> "Left";
            case Keys.RIGHT -> "Right";
            case Keys.UP -> "Up";
            case Keys.DOWN -> "Down";
            default -> "#" + keyCode;
        };
    }
}
