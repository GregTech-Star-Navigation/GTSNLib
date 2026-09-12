package com.gtsn.lib.ui.screen;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.widget.BoxWidget;
import com.gtsn.lib.ui.widget.Stack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 控件泊点行为：屏幕尺寸驱动布局、重新布局、渲染委托。
 */
class WidgetHostTest {

    @Test
    void resizeLaysOutRootToScreenAndChildrenInOrder() {
        Stack root = Stack.vertical().padding(Insets.all(4)).gap(2);
        BoxWidget a = root.add(new BoxWidget().fixedSize(30, 10));
        BoxWidget b = root.add(new BoxWidget().fixedSize(30, 10));
        WidgetHost host = new WidgetHost(root);

        host.resize(120, 80);

        assertEquals(120, host.width());
        assertEquals(80, host.height());
        assertEquals(Rect.of(0, 0, 120, 80), root.bounds());
        assertEquals(Rect.of(4, 4, 30, 10), a.bounds());
        assertEquals(Rect.of(4, 16, 30, 10), b.bounds());
    }

    @Test
    void resizeAgainRelayoutsWeightedChild() {
        Stack root = Stack.vertical();
        root.add(new BoxWidget().fixedSize(20, 10));
        BoxWidget body = root.add(new BoxWidget().size(Sizing.fill(), Sizing.wrap()).weight(1));
        WidgetHost host = new WidgetHost(root);

        host.resize(100, 50);
        assertEquals(Rect.of(0, 10, 100, 40), body.bounds());

        host.resize(100, 90);
        assertEquals(Rect.of(0, 10, 100, 80), body.bounds(), "重新布局随屏幕尺寸分配剩余空间");
    }

    @Test
    void renderDelegatesToRootWidget() {
        Stack root = Stack.vertical();
        root.add(new BoxWidget().fixedSize(10, 10).fill(0xFF010203));
        WidgetHost host = new WidgetHost(root);
        host.resize(50, 50);
        RecordingRenderContext ctx = new RecordingRenderContext(50, 50);

        host.render(ctx);

        assertEquals(List.of("fill(0,0,10,10,ff010203)"), ctx.ops);
    }
}
