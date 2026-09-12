package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.input.Keys;
import com.gtsn.lib.ui.screen.WidgetHost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 按钮交互状态机：NORMAL / HOVERED / PRESSED / DISABLED 的进入与退出。
 */
class ButtonStateTest {

    private ButtonWidget button;
    private WidgetHost host;
    private int clicks;

    @BeforeEach
    void setUp() {
        clicks = 0;
        Stack root = Stack.vertical();
        button = root.add(new ButtonWidget("ok", PlainTextMetrics.INSTANCE, () -> clicks++)
                .fixedSize(40, 20));
        host = new WidgetHost(root);
        host.resize(100, 50);
    }

    @Test
    void stateStartsNormal() {
        assertEquals(ButtonState.NORMAL, button.state());
        assertTrue(button.isEnabled());
        assertFalse(button.isHovered());
        assertFalse(button.isPressed());
    }

    @Test
    void hoverEntersAndLeavesHoverState() {
        host.dispatch(new InputEvent.MouseMoved(10, 10));
        assertEquals(ButtonState.HOVERED, button.state());

        host.dispatch(new InputEvent.MouseMoved(90, 45));
        assertEquals(ButtonState.NORMAL, button.state());
    }

    @Test
    void pressWinsOverHoverUntilRelease() {
        host.dispatch(new InputEvent.MouseMoved(10, 10));
        host.dispatch(new InputEvent.MousePressed(10, 10, 0));
        assertEquals(ButtonState.PRESSED, button.state());

        host.dispatch(new InputEvent.MouseReleased(10, 10, 0));
        assertEquals(ButtonState.HOVERED, button.state());
        assertEquals(1, clicks);
    }

    @Test
    void releaseOutsideCancelsClickAndKeepsHover() {
        host.dispatch(new InputEvent.MouseMoved(10, 10));
        host.dispatch(new InputEvent.MousePressed(10, 10, 0));
        host.dispatch(new InputEvent.MouseReleased(90, 45, 0));

        assertEquals(0, clicks);
        assertEquals(ButtonState.HOVERED, button.state(), "无移动事件时悬停保持");
    }

    @Test
    void disabledWinsOverAllOtherStatesAndBlocksActivation() {
        host.dispatch(new InputEvent.MouseMoved(10, 10));
        button.enabled(false);

        assertEquals(ButtonState.DISABLED, button.state());
        assertFalse(button.isFocusable());
        host.dispatch(new InputEvent.MousePressed(10, 10, 0));
        host.dispatch(new InputEvent.MouseReleased(10, 10, 0));
        assertEquals(0, clicks, "禁用按钮不响应鼠标");
    }

    @Test
    void focusedButtonActivatesWithKeyboardAndReportsFocus() {
        host.router().focus().requestFocus(button);
        assertTrue(button.isFocused());

        assertTrue(host.dispatch(new InputEvent.KeyPressed(Keys.ENTER, 0, 0)));
        assertEquals(1, clicks);
    }
}
