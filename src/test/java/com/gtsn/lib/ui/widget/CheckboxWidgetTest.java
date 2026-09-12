package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.input.Keys;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.screen.WidgetHost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 复选框：点击/键盘切换、回调、禁用与悬停/按下状态、勾选标记绘制。
 */
class CheckboxWidgetTest {

    private CheckboxWidget checkbox;
    private WidgetHost host;
    private final List<Boolean> changes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        changes.clear();
        Stack root = Stack.vertical();
        checkbox = root.add(new CheckboxWidget("启用", PlainTextMetrics.INSTANCE, changes::add)
                .fixedSize(80, 12));
        host = new WidgetHost(root);
        host.resize(120, 40);
    }

    private void click(WidgetHost target, double x, double y) {
        target.dispatch(new InputEvent.MousePressed(x, y, 0));
        target.dispatch(new InputEvent.MouseReleased(x, y, 0));
    }

    @Test
    void clickTogglesCheckedAndFiresCallback() {
        assertFalse(checkbox.checked());

        click(host, 6, 6);
        assertTrue(checkbox.checked());
        assertEquals(List.of(true), changes);

        click(host, 6, 6);
        assertFalse(checkbox.checked());
        assertEquals(List.of(true, false), changes);
    }

    @Test
    void releaseOutsideDoesNotToggle() {
        host.dispatch(new InputEvent.MousePressed(6, 6, 0));
        host.dispatch(new InputEvent.MouseReleased(110, 35, 0));

        assertFalse(checkbox.checked());
        assertTrue(changes.isEmpty());
    }

    @Test
    void keyboardSpaceAndEnterToggleWhenFocused() {
        host.router().focus().requestFocus(checkbox);
        assertTrue(checkbox.isFocused());

        assertTrue(host.dispatch(new InputEvent.KeyPressed(Keys.SPACE, 0, 0)));
        assertTrue(checkbox.checked());

        assertTrue(host.dispatch(new InputEvent.KeyPressed(Keys.ENTER, 0, 0)));
        assertFalse(checkbox.checked());

        assertFalse(host.dispatch(new InputEvent.CharTyped('x', 0)), "字符输入不触发切换");
    }

    @Test
    void disabledCheckboxIgnoresInputAndFocus() {
        checkbox.enabled(false);

        assertFalse(checkbox.isFocusable());
        click(host, 6, 6);

        assertFalse(checkbox.checked());
        assertTrue(changes.isEmpty());
    }

    @Test
    void stateFlagsTrackHoverPressAndFocus() {
        host.dispatch(new InputEvent.MouseMoved(6, 6));
        assertTrue(checkbox.isHovered());

        host.dispatch(new InputEvent.MousePressed(6, 6, 0));
        assertTrue(checkbox.isPressed());

        host.dispatch(new InputEvent.MouseReleased(6, 6, 0));
        assertFalse(checkbox.isPressed());

        host.dispatch(new InputEvent.MouseMoved(110, 35));
        assertFalse(checkbox.isHovered());
    }

    @Test
    void checkedCheckboxPaintsCheckMark() {
        Stack root = Stack.vertical();
        CheckboxWidget widget = root.add(new CheckboxWidget("x", PlainTextMetrics.INSTANCE, value -> {
        }).fixedSize(40, 12));
        WidgetHost localHost = new WidgetHost(root);
        localHost.resize(60, 30);
        RecordingRenderContext ctx = new RecordingRenderContext(60, 30);

        localHost.render(ctx);
        int uncheckedFills = ctx.ops.size();

        widget.checked(true);
        ctx.ops.clear();
        localHost.render(ctx);

        assertTrue(ctx.ops.size() > uncheckedFills, "勾选后应额外绘制勾选标记");
        assertTrue(ctx.ops.stream().anyMatch(op -> op.startsWith("text(x@")), ctx.ops.toString());
    }

    @Test
    void checkboxWrapsLabelIntrinsicSize() {
        Stack root = Stack.vertical();
        CheckboxWidget widget = root.add(new CheckboxWidget("abcd", PlainTextMetrics.INSTANCE, value -> {
        }));
        WidgetHost localHost = new WidgetHost(root);
        localHost.resize(100, 30);

        Rect bounds = widget.bounds();
        assertEquals(12 + 4 + 24, bounds.width(), "盒宽+间隙+文本宽");
        assertEquals(12, bounds.height(), "盒高与行高取大者");
    }
}
