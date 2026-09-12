package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.screen.WidgetHost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 滚动容器：内容测量/偏移、滚轮与滚动条拖拽、边界钳制、裁剪与滚动后的命中测试。
 */
class ScrollPanelWidgetTest {

    private ScrollPanelWidget panel;
    private Stack content;
    private BoxWidget[] rows;
    private WidgetHost host;

    @BeforeEach
    void setUp() {
        Stack root = Stack.vertical();
        panel = root.add(new ScrollPanelWidget().fixedSize(100, 50));
        content = panel.add(Stack.vertical().gap(2));
        rows = new BoxWidget[10];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = content.add(new BoxWidget().fixedSize(80, 20).fill(0xFF202020 + i));
        }
        host = new WidgetHost(root);
        host.resize(100, 50);
    }

    @Test
    void contentHeightAndMaxScrollReflectOverflow() {
        assertEquals(10 * 20 + 9 * 2, panel.contentHeight(), "内容高=10*20+9*2");
        assertEquals(168, panel.maxScroll(), "最大滚动=218-50");
        assertEquals(0, panel.scrollY());
        assertEquals(0, content.bounds().y());
        assertEquals(Rect.of(0, 0, 94, 50), panel.viewportBounds(), "视口宽度扣除滚动条");
    }

    @Test
    void wheelScrollsContentAndClampsToBothEnds() {
        boolean consumed = host.dispatch(new InputEvent.MouseScrolled(50, 25, 0, -1));
        assertTrue(consumed, "悬停视口内滚轮事件被消费");
        assertEquals(panel.scrollStep(), panel.scrollY());
        assertEquals(-panel.scrollStep(), content.bounds().y());

        for (int i = 0; i < 50; i++) {
            host.dispatch(new InputEvent.MouseScrolled(50, 25, 0, -1));
        }
        assertEquals(panel.maxScroll(), panel.scrollY(), "向下滚动到底部即停");
        assertEquals(-panel.maxScroll(), content.bounds().y());

        for (int i = 0; i < 50; i++) {
            host.dispatch(new InputEvent.MouseScrolled(50, 25, 0, 1));
        }
        assertEquals(0, panel.scrollY(), "向上滚动到顶部即停");
    }

    @Test
    void contentShorterThanViewportCannotScroll() {
        Stack root = Stack.vertical();
        ScrollPanelWidget small = root.add(new ScrollPanelWidget().fixedSize(100, 50));
        small.add(new BoxWidget().fixedSize(80, 20));
        WidgetHost smallHost = new WidgetHost(root);
        smallHost.resize(100, 50);

        assertEquals(0, small.maxScroll());
        assertFalse(smallHost.dispatch(new InputEvent.MouseScrolled(50, 25, 0, -1)), "无溢出时不消费滚轮");
        assertEquals(0, small.scrollY());
        assertEquals(0, small.thumbBounds().height(), "无溢出时不显示滚动条滑块");
    }

    @Test
    void scrollingShiftsChildBoundsAndHitTestingFollows() {
        panel.scrollTo(30);

        assertEquals(-30, rows[0].bounds().y(), "首行整体移出视口上方");
        assertEquals(-8, rows[1].bounds().y(), "第二行部分可见");

        host.dispatch(new InputEvent.MousePressed(5, 0, 0));
        assertSame(rows[1], host.router().pressedWidget().orElseThrow(),
                "滚动后命中测试跟随移动后的子控件包围盒");

        host.dispatch(new InputEvent.MouseReleased(5, 0, 0));
        assertEquals(30, panel.scrollY());
    }

    @Test
    void draggingThumbScrollsProportionallyAndClamps() {
        Rect thumb = panel.thumbBounds();
        assertTrue(thumb.height() > 0, "溢出时显示滑块");

        int thumbCenterX = thumb.x() + thumb.width() / 2;
        int thumbCenterY = thumb.y() + thumb.height() / 2;
        host.dispatch(new InputEvent.MousePressed(thumbCenterX, thumbCenterY, 0));
        assertTrue(panel.isDraggingThumb());

        int travel = panel.trackBounds().height() - thumb.height();
        host.dispatch(new InputEvent.MouseDragged(thumbCenterX, thumbCenterY + travel, 0, 0, travel));
        assertEquals(panel.maxScroll(), panel.scrollY(), "拖到底部对应滚动到底");

        host.dispatch(new InputEvent.MouseReleased(thumbCenterX, thumbCenterY + travel, 0));
        assertFalse(panel.isDraggingThumb());
    }

    @Test
    void renderClipsChildrenToViewportAndDrawsScrollbar() {
        RecordingRenderContext ctx = new RecordingRenderContext(100, 50);

        host.render(ctx);

        assertTrue(ctx.ops.contains("pushClip(0,0,94,50)"), "子控件渲染被视口裁剪: " + ctx.ops);
        assertTrue(ctx.ops.contains("popClip"), ctx.ops.toString());
        int clipIndex = ctx.ops.indexOf("pushClip(0,0,94,50)");
        int popIndex = ctx.ops.indexOf("popClip");
        assertTrue(ctx.ops.subList(clipIndex + 1, popIndex).stream().anyMatch(op -> op.startsWith("fill(0,")),
                "子控件在裁剪区间内绘制: " + ctx.ops);
    }

    @Test
    void relayoutOnResizeClampsExistingScroll() {
        panel.scrollTo(panel.maxScroll());
        assertEquals(168, panel.scrollY());

        panel.size(com.gtsn.lib.ui.layout.Sizing.fixed(100), com.gtsn.lib.ui.layout.Sizing.fixed(200));
        host.resize(100, 200);

        assertEquals(18, panel.maxScroll(), "视口变高后最大滚动变小");
        assertEquals(18, panel.scrollY(), "已有滚动值被钳制到新上限");
    }

    @Test
    void contentCanBeMeasuredTallerThanViewportWithoutClamping() {
        assertTrue(panel.contentHeight() > panel.viewportBounds().height(),
                "内容测量不受视口高度 AT_MOST 钳制");
    }
}
