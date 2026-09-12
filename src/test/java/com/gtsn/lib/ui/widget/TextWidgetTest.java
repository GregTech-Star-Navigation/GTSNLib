package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.screen.WidgetHost;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文本控件的换行与对齐：按词换行、超长单词硬断、显式换行、行距与行内对齐（无 MC）。
 */
class TextWidgetTest {

    @Test
    void wrapBreaksAtWordBoundary() {
        Stack root = Stack.vertical();
        TextWidget text = root.add(new TextWidget("aaa bbb ccc", PlainTextMetrics.INSTANCE).wrap(60));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);

        assertEquals(List.of("aaa bbb", "ccc"), text.lines());
        assertEquals(Rect.of(0, 0, 42, 18), text.bounds(), "宽度取最长行，高度=行数*行高");
    }

    @Test
    void wrapHardBreaksWhenSingleWordExceedsWidth() {
        Stack root = Stack.vertical();
        TextWidget text = root.add(new TextWidget("abcdefgh", PlainTextMetrics.INSTANCE).wrap(24));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);

        assertEquals(List.of("abcd", "efgh"), text.lines());
        assertEquals(Rect.of(0, 0, 24, 18), text.bounds());
    }

    @Test
    void wrapHonoursExplicitNewlines() {
        Stack root = Stack.vertical();
        TextWidget text = root.add(new TextWidget("ab\ncd", PlainTextMetrics.INSTANCE).wrap(60));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);

        assertEquals(List.of("ab", "cd"), text.lines());
        assertEquals(Rect.of(0, 0, 12, 18), text.bounds());
    }

    @Test
    void lineSpacingAddsExtraHeightBetweenLines() {
        Stack root = Stack.vertical();
        TextWidget text = root.add(new TextWidget("aa bb", PlainTextMetrics.INSTANCE).wrap(24).lineSpacing(5));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);

        assertEquals(List.of("aa", "bb"), text.lines());
        assertEquals(Rect.of(0, 0, 12, 23), text.bounds(), "高度=2*9+5");
    }

    @Test
    void centeredAlignmentPositionsLineWithinFixedBox() {
        Stack root = Stack.vertical();
        TextWidget text = root.add(new TextWidget("ab", PlainTextMetrics.INSTANCE)
                .size(Sizing.fixed(60), Sizing.wrap())
                .align(TextAlign.CENTER));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);
        RecordingRenderContext ctx = new RecordingRenderContext(200, 100);

        host.render(ctx);

        assertEquals(Rect.of(0, 0, 60, 9), text.bounds());
        assertTrue(ctx.ops.contains("text(ab@24,0,ffe6e6e6,shadow=false)"),
                "居中对齐时行起点为 (60-12)/2=24: " + ctx.ops);
    }

    @Test
    void rightAlignmentPinsLineToBoxRightEdge() {
        Stack root = Stack.vertical();
        root.add(new TextWidget("ab", PlainTextMetrics.INSTANCE)
                .size(Sizing.fixed(60), Sizing.wrap())
                .align(TextAlign.RIGHT));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);
        RecordingRenderContext ctx = new RecordingRenderContext(200, 100);

        host.render(ctx);

        assertTrue(ctx.ops.contains("text(ab@48,0,ffe6e6e6,shadow=false)"), "右对齐: " + ctx.ops);
    }

    @Test
    void multilineRendersEachLineAtLineHeight() {
        Stack root = Stack.vertical();
        root.add(new TextWidget("aa bb", PlainTextMetrics.INSTANCE).wrap(24));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);
        RecordingRenderContext ctx = new RecordingRenderContext(200, 100);

        host.render(ctx);

        assertTrue(ctx.ops.contains("text(aa@0,0,ffe6e6e6,shadow=false)"), ctx.ops.toString());
        assertTrue(ctx.ops.contains("text(bb@0,9,ffe6e6e6,shadow=false)"), ctx.ops.toString());
    }
}
