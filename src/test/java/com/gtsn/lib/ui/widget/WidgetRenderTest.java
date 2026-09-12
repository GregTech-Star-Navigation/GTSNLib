package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.input.Keys;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.screen.WidgetHost;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 控件行为：渲染顺序、裁剪、固有尺寸、按钮鼠标/键盘交互与悬停视觉。
 */
class WidgetRenderTest {

    @Test
    void renderPaintsParentBeforeChildrenInListOrder() {
        Stack root = Stack.vertical().padding(Insets.all(2));
        root.add(new BoxWidget().fixedSize(40, 20).fill(0xFF112233));
        root.add(new TextWidget("hi", PlainTextMetrics.INSTANCE));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);
        RecordingRenderContext ctx = new RecordingRenderContext(100, 50);

        host.render(ctx);

        assertEquals("fill(2,2,40,20,ff112233)", ctx.ops.get(0), "父容器先于子控件绘制");
        assertEquals("text(hi@2,22,ffe6e6e6,shadow=false)", ctx.ops.get(1));
    }

    @Test
    void clipWidgetWrapsChildrenInScissor() {
        Stack root = Stack.vertical();
        ClipWidget clip = root.add(new ClipWidget().fixedSize(30, 10));
        clip.add(new BoxWidget().fixedSize(100, 100).fill(0xFFAA0000));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);
        RecordingRenderContext ctx = new RecordingRenderContext(100, 50);

        host.render(ctx);

        assertEquals(List.of("pushClip(0,0,30,10)", "fill(0,0,100,100,ffaa0000)", "popClip"), ctx.ops,
                "子控件绘制被 scissor 包裹（溢出部分由渲染层裁掉）");
    }

    @Test
    void textWidgetWrapsIntrinsicSizeFromMetrics() {
        Stack root = Stack.vertical().padding(Insets.all(3));
        TextWidget text = root.add(new TextWidget("abcd", PlainTextMetrics.INSTANCE));
        WidgetHost host = new WidgetHost(root);

        host.resize(100, 50);

        assertEquals(Rect.of(3, 3, 24, 9), text.bounds(), "宽度=字符数*6，高度=行高");
    }

    @Test
    void buttonWrapsIntrinsicSizeAroundLabelAndPadding() {
        Stack root = Stack.vertical();
        ButtonWidget button = root.add(new ButtonWidget("ok", PlainTextMetrics.INSTANCE, () -> {
        }));
        WidgetHost host = new WidgetHost(root);

        host.resize(100, 50);

        assertEquals(Rect.of(0, 0, 24, 15), button.bounds(), "宽度=文本+左右 padding，高度=行高+上下 padding");
    }

    @Test
    void buttonFiresOnClickInsideAndNotOutside() {
        AtomicInteger clicks = new AtomicInteger();
        Stack root = Stack.vertical();
        root.add(new ButtonWidget("ok", PlainTextMetrics.INSTANCE, clicks::incrementAndGet).fixedSize(40, 20));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);

        host.dispatch(new InputEvent.MousePressed(10, 10, 0));
        host.dispatch(new InputEvent.MouseReleased(10, 10, 0));
        assertEquals(1, clicks.get(), "边界内按下并释放触发一次点击");

        host.dispatch(new InputEvent.MousePressed(10, 10, 0));
        host.dispatch(new InputEvent.MouseReleased(90, 45, 0));
        assertEquals(1, clicks.get(), "释放在边界外不触发");

        host.dispatch(new InputEvent.MousePressed(90, 45, 0));
        host.dispatch(new InputEvent.MouseReleased(90, 45, 0));
        assertEquals(1, clicks.get(), "未在按钮上按下不触发");
    }

    @Test
    void focusedButtonActivatesWithEnterAndSpace() {
        AtomicInteger clicks = new AtomicInteger();
        Stack root = Stack.vertical();
        ButtonWidget button = root.add(
                new ButtonWidget("ok", PlainTextMetrics.INSTANCE, clicks::incrementAndGet).fixedSize(40, 20));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);
        host.router().focus().requestFocus(button);

        assertTrue(host.dispatch(new InputEvent.KeyPressed(Keys.ENTER, 0, 0)));
        assertTrue(host.dispatch(new InputEvent.KeyPressed(Keys.SPACE, 0, 0)));
        assertEquals(2, clicks.get());
        assertFalse(host.dispatch(new InputEvent.CharTyped('x', 0)), "字符输入不触发按钮");
        assertEquals(2, clicks.get());
    }

    @Test
    void hoveredButtonRendersDifferentFillColor() {
        Stack root = Stack.vertical();
        root.add(new ButtonWidget("ok", PlainTextMetrics.INSTANCE, () -> {
        }).fixedSize(40, 20));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);
        RecordingRenderContext ctx = new RecordingRenderContext(100, 50);

        host.render(ctx);
        String idle = ctx.ops.get(0);

        host.dispatch(new InputEvent.MouseMoved(10, 10));
        ctx.ops.clear();
        host.render(ctx);
        String hover = ctx.ops.get(0);

        assertNotEquals(idle, hover, "悬停态背景色应与静止态不同: " + idle + " vs " + hover);
    }
}
