package com.gtsn.lib.ui.demo;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.screen.WidgetHost;
import com.gtsn.lib.ui.widget.PlainTextMetrics;
import com.gtsn.lib.ui.widget.Widget;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 开发测试界面装配行为：布局、锚定、按钮状态更新、键盘焦点与裁剪演示。
 */
class DemoContentTest {

    private final RecordingRenderContext ctx = new RecordingRenderContext(320, 240);
    private DemoContent demo;
    private WidgetHost host;

    private void setUp() {
        demo = DemoContent.build(PlainTextMetrics.INSTANCE);
        host = new WidgetHost(demo.root());
        host.resize(320, 240);
    }

    private void click(Widget widget) {
        Rect bounds = widget.bounds();
        double x = bounds.x() + bounds.width() / 2.0;
        double y = bounds.y() + bounds.height() / 2.0;
        host.dispatch(new InputEvent.MousePressed(x, y, 0));
        host.dispatch(new InputEvent.MouseReleased(x, y, 0));
    }

    @Test
    void rootFillsScreenAndBadgeAnchorsToBottomRight() {
        setUp();

        assertEquals(Rect.of(0, 0, 320, 240), demo.root().bounds());
        Rect root = demo.root().node().contentBounds();
        Rect badge = demo.badge().bounds();
        assertEquals(root.right() - badge.width() - 4, badge.x());
        assertEquals(root.bottom() - badge.height() - 4, badge.y());
    }

    @Test
    void clickButtonIncrementsStateAndStatusText() {
        setUp();

        click(demo.clickButton());

        assertEquals(1, demo.state().clicks());
        assertTrue(demo.clickStatus().text().contains("1"), demo.clickStatus().text());
    }

    @Test
    void toggleButtonFlipsStateAndStatusText() {
        setUp();

        click(demo.toggleButton());

        assertTrue(demo.state().toggled());
        assertTrue(demo.toggleStatus().text().contains("B"), demo.toggleStatus().text());
    }

    @Test
    void keypadReceivesKeysAfterClickFocusAndClearResets() {
        setUp();

        click(demo.keypad());
        assertTrue(host.router().focus().isFocused(demo.keypad()));

        host.dispatch(new InputEvent.KeyPressed('K', 0, 0));
        host.dispatch(new InputEvent.CharTyped('x', 0));
        host.dispatch(new InputEvent.CharTyped('y', 0));

        assertEquals("K", demo.state().lastKey());
        assertEquals("xy", demo.state().typed());

        click(demo.clearButton());

        assertEquals("(none)", demo.state().lastKey());
        assertEquals("", demo.state().typed());
    }

    @Test
    void clipShowcaseContainsOverflowingChild() {
        setUp();

        Rect show = demo.clipShowcase().bounds();
        assertTrue(descendantExceedsWidth(demo.clipShowcase(), show.width()), "裁剪演示必须包含一个溢出子节点");
    }

    @Test
    void demoRendersHeadlessly() {
        setUp();

        host.render(ctx);

        assertFalse(ctx.ops.isEmpty());
        assertTrue(ctx.ops.contains("pushClip(" + demo.clipShowcase().bounds().x() + ","
                + demo.clipShowcase().bounds().y() + ","
                + demo.clipShowcase().bounds().width() + ","
                + demo.clipShowcase().bounds().height() + ")"), "裁剪容器渲染时必须启用 scissor");
    }

    private static boolean descendantExceedsWidth(Widget widget, int width) {
        for (Widget child : widget.children()) {
            if (child.bounds().width() > width || descendantExceedsWidth(child, width)) {
                return true;
            }
        }
        return false;
    }
}
