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
 * 开关：与复选框一致的切换语义，额外的滑块位置状态。
 */
class ToggleSwitchWidgetTest {

    private ToggleSwitchWidget toggle;
    private WidgetHost host;
    private final List<Boolean> changes = new ArrayList<>();

    @BeforeEach
    void setUp() {
        changes.clear();
        Stack root = Stack.vertical();
        toggle = root.add(new ToggleSwitchWidget("自动", PlainTextMetrics.INSTANCE, changes::add)
                .fixedSize(90, 14));
        host = new WidgetHost(root);
        host.resize(120, 40);
    }

    private void click(double x, double y) {
        host.dispatch(new InputEvent.MousePressed(x, y, 0));
        host.dispatch(new InputEvent.MouseReleased(x, y, 0));
    }

    @Test
    void clickTogglesAndKnobMovesToOnPosition() {
        Rect off = toggle.knobBounds();
        assertEquals(0.0, toggle.knobPosition(), 1e-9);

        click(12, 7);

        assertTrue(toggle.checked());
        assertEquals(List.of(true), changes);
        assertEquals(1.0, toggle.knobPosition(), 1e-9);

        Rect on = toggle.knobBounds();
        int expectedTravel = 24 - 2 * 2 - (12 - 2 * 2);
        assertEquals(expectedTravel, on.x() - off.x(), "滑块横移距离=轨道内可移动空间");
        assertEquals(off.y(), on.y());
    }

    @Test
    void keyboardTogglesWhenFocused() {
        host.router().focus().requestFocus(toggle);

        assertTrue(host.dispatch(new InputEvent.KeyPressed(Keys.SPACE, 0, 0)));
        assertTrue(toggle.checked());
        assertTrue(host.dispatch(new InputEvent.KeyPressed(Keys.ENTER, 0, 0)));
        assertFalse(toggle.checked());
    }

    @Test
    void disabledToggleIgnoresInput() {
        toggle.enabled(false);

        click(12, 7);

        assertFalse(toggle.checked());
        assertFalse(toggle.isFocusable());
        assertTrue(changes.isEmpty());
    }

    @Test
    void hoverAndPressStatesAreTracked() {
        host.dispatch(new InputEvent.MouseMoved(12, 7));
        assertTrue(toggle.isHovered());

        host.dispatch(new InputEvent.MousePressed(12, 7, 0));
        assertTrue(toggle.isPressed());

        host.dispatch(new InputEvent.MouseReleased(12, 7, 0));
        assertFalse(toggle.isPressed());
    }

    @Test
    void checkedToggleRendersDifferentTrackColor() {
        Stack root = Stack.vertical();
        ToggleSwitchWidget widget = root.add(new ToggleSwitchWidget("x", PlainTextMetrics.INSTANCE, value -> {
        }).fixedSize(60, 14));
        WidgetHost localHost = new WidgetHost(root);
        localHost.resize(80, 30);
        RecordingRenderContext ctx = new RecordingRenderContext(80, 30);

        localHost.render(ctx);
        String offTrack = ctx.ops.get(0);

        widget.checked(true);
        ctx.ops.clear();
        localHost.render(ctx);

        assertFalse(offTrack.equals(ctx.ops.get(0)), "开/关轨道颜色应不同: " + ctx.ops);
    }
}
