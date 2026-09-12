package com.gtsn.lib.ui.screen;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.widget.BoxWidget;
import com.gtsn.lib.ui.widget.ButtonWidget;
import com.gtsn.lib.ui.widget.PanelWidget;
import com.gtsn.lib.ui.widget.PlainTextMetrics;
import com.gtsn.lib.ui.widget.Stack;
import com.gtsn.lib.ui.widget.Tooltip;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工具提示：悬停解析（含最近祖先）、鼠标跟随、渲染盒与边界钳制。
 */
class TooltipOverlayTest {

    private static final int WIDTH = 100;
    private static final int HEIGHT = 50;

    @Test
    void hoveredWidgetTooltipBecomesActiveAndClearsOnLeave() {
        Stack root = Stack.vertical();
        root.add(new ButtonWidget("ok", PlainTextMetrics.INSTANCE, () -> {
        }).fixedSize(40, 20).tooltip(Tooltip.of("提示一", "提示二")));
        WidgetHost host = new WidgetHost(root);
        host.resize(WIDTH, HEIGHT);

        assertTrue(host.activeTooltip().isEmpty());

        host.dispatch(new InputEvent.MouseMoved(10, 10));
        assertEquals(List.of("提示一", "提示二"), host.activeTooltip().orElseThrow().lines());

        host.dispatch(new InputEvent.MouseMoved(90, 45));
        assertTrue(host.activeTooltip().isEmpty());
    }

    @Test
    void tooltipResolvesThroughNearestTooltippedAncestor() {
        Stack root = Stack.vertical();
        PanelWidget panel = root.add(new PanelWidget().fixedSize(80, 40).tooltip(Tooltip.of("面板提示")));
        panel.add(new BoxWidget().fixedSize(20, 20));
        WidgetHost host = new WidgetHost(root);
        host.resize(WIDTH, HEIGHT);

        host.dispatch(new InputEvent.MouseMoved(5, 5));

        assertEquals(List.of("面板提示"), host.activeTooltip().orElseThrow().lines(),
                "子控件无提示时向上解析到面板");
    }

    @Test
    void renderTooltipsDrawsBoxAndLines() {
        Stack root = Stack.vertical();
        root.add(new ButtonWidget("ok", PlainTextMetrics.INSTANCE, () -> {
        }).fixedSize(40, 20).tooltip(Tooltip.of("提示一", "提示二")));
        WidgetHost host = new WidgetHost(root);
        host.resize(WIDTH, HEIGHT);
        host.dispatch(new InputEvent.MouseMoved(10, 10));
        RecordingRenderContext ctx = new RecordingRenderContext(WIDTH, HEIGHT);

        host.renderTooltips(ctx);

        assertFalse(ctx.ops.isEmpty(), "悬停有提示的控件时渲染覆盖层");
        assertTrue(ctx.ops.stream().anyMatch(op -> op.startsWith("text(提示一@")), ctx.ops.toString());
        assertTrue(ctx.ops.stream().anyMatch(op -> op.startsWith("text(提示二@")), ctx.ops.toString());
        assertTrue(ctx.ops.size() >= 6, "背景+边框+文本: " + ctx.ops);
    }

    @Test
    void renderTooltipsIsNoOpWithoutHover() {
        Stack root = Stack.vertical();
        root.add(new BoxWidget().fixedSize(20, 20));
        WidgetHost host = new WidgetHost(root);
        host.resize(WIDTH, HEIGHT);
        RecordingRenderContext ctx = new RecordingRenderContext(WIDTH, HEIGHT);

        host.renderTooltips(ctx);

        assertTrue(ctx.ops.isEmpty());
    }

    @Test
    void tooltipBoundsClampInsideScreenNearEdges() {
        Stack root = Stack.vertical();
        root.add(new ButtonWidget("ok", PlainTextMetrics.INSTANCE, () -> {
        }).fixedSize(WIDTH, HEIGHT).tooltip(Tooltip.of("很长的提示文本很长")));
        WidgetHost host = new WidgetHost(root);
        host.resize(WIDTH, HEIGHT);
        host.dispatch(new InputEvent.MouseMoved(WIDTH - 1, HEIGHT - 1));
        RecordingRenderContext ctx = new RecordingRenderContext(WIDTH, HEIGHT);

        Rect bounds = host.tooltipBounds(ctx).orElseThrow();

        assertTrue(bounds.x() >= 0 && bounds.y() >= 0, "不得越出左上边界: " + bounds);
        assertTrue(bounds.right() <= WIDTH && bounds.bottom() <= HEIGHT, "不得越出右下边界: " + bounds);
    }

    @Test
    void tooltipLinesAreImmutable() {
        Tooltip tooltip = Tooltip.of("a", "b");

        assertThrows(UnsupportedOperationException.class, () -> tooltip.lines().add("c"));
    }
}
