package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.screen.WidgetHost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 占位控件：固定尺寸占位与权重占位，且自身不产生任何绘制。
 */
class SpacerWidgetTest {

    @Test
    void fixedSpacerOccupiesExactSpaceInRow() {
        Stack root = Stack.horizontal();
        root.add(new BoxWidget().fixedSize(10, 10));
        SpacerWidget spacer = root.add(new SpacerWidget().fixedSize(15, 10));
        BoxWidget after = root.add(new BoxWidget().fixedSize(10, 10));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 20);

        assertEquals(Rect.of(10, 0, 15, 10), spacer.bounds());
        assertEquals(25, after.bounds().x());
    }

    @Test
    void weightedSpacerConsumesRemainingMainAxis() {
        Stack root = Stack.horizontal();
        root.add(new BoxWidget().fixedSize(10, 10));
        SpacerWidget spacer = root.add(new SpacerWidget().weight(1));
        root.add(new BoxWidget().fixedSize(10, 10));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 20);

        assertEquals(10, spacer.bounds().x());
        assertEquals(80, spacer.bounds().width(), "权重 1 的占位吃掉剩余 80px");
        assertEquals(90, root.children().get(2).bounds().x());
    }

    @Test
    void spacerRendersNothing() {
        Stack root = Stack.vertical();
        root.add(new SpacerWidget().weight(1));
        WidgetHost host = new WidgetHost(root);
        host.resize(50, 50);
        RecordingRenderContext ctx = new RecordingRenderContext(50, 50);

        host.render(ctx);

        assertTrue(ctx.ops.isEmpty(), "占位控件不应产生绘制调用: " + ctx.ops);
    }
}
