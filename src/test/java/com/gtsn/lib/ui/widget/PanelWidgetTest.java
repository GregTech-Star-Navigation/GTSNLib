package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.screen.WidgetHost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 面板容器：背景/边框绘制顺序、内边距与间距布局、标题头预留与绘制。
 */
class PanelWidgetTest {

    @Test
    void panelPaintsBackgroundAndBorderBeforeChildren() {
        Stack root = Stack.vertical();
        PanelWidget panel = root.add(new PanelWidget().background(0xFF101820).border(0xFF4890C8, 1)
                .padding(Insets.all(4)).gap(3));
        BoxWidget first = panel.add(new BoxWidget().fixedSize(20, 10).fill(0xFFAA0000));
        BoxWidget second = panel.add(new BoxWidget().fixedSize(20, 10).fill(0xFF00AA00));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 80);
        RecordingRenderContext ctx = new RecordingRenderContext(100, 80);

        host.render(ctx);

        assertEquals(Rect.of(0, 0, 28, 31), panel.bounds());
        assertEquals(Rect.of(4, 4, 20, 10), first.bounds());
        assertEquals(Rect.of(4, 17, 20, 10), second.bounds());
        assertEquals("fill(0,0,28,31,ff101820)", ctx.ops.get(0), "背景先于边框与子控件");
        assertEquals("fill(4,4,20,10,ffaa0000)", ctx.ops.get(5), "4 次边框填充后才是第一个子控件: " + ctx.ops);
    }

    @Test
    void panelTitleReservesHeaderSpaceAndRendersTitle() {
        Stack root = Stack.vertical();
        PanelWidget panel = root.add(new PanelWidget().title("标题", PlainTextMetrics.INSTANCE)
                .padding(Insets.all(4)));
        BoxWidget child = panel.add(new BoxWidget().fixedSize(20, 10));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 100);
        RecordingRenderContext ctx = new RecordingRenderContext(100, 100);

        host.render(ctx);

        int header = PlainTextMetrics.INSTANCE.lineHeight() + 6;
        assertEquals(4 + header, child.bounds().y(), "标题头把内容推下");
        assertEquals(4 + header + 10 + 4, panel.bounds().height());
        assertTrue(ctx.ops.stream().anyMatch(op -> op.startsWith("text(标题@4,7,")),
                "标题文本绘制在头部垂直居中: " + ctx.ops);
    }

    @Test
    void panelWithoutBackgroundSkipsFill() {
        Stack root = Stack.vertical();
        PanelWidget panel = root.add(new PanelWidget().padding(Insets.all(2)));
        panel.add(new BoxWidget().fixedSize(10, 10).fill(0xFF010203));
        WidgetHost host = new WidgetHost(root);
        host.resize(50, 50);
        RecordingRenderContext ctx = new RecordingRenderContext(50, 50);

        host.render(ctx);

        assertEquals("fill(2,2,10,10,ff010203)", ctx.ops.get(0), "无背景/边框时首个绘制来自子控件");
    }
}
