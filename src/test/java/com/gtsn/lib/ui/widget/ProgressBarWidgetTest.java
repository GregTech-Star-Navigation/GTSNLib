package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.screen.WidgetHost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 进度条：数值区间钳制、比例映射、比例填充宽度与标签绘制。
 */
class ProgressBarWidgetTest {

    @Test
    void valueClampsBelowAndAboveRange() {
        ProgressBarWidget bar = new ProgressBarWidget().range(0, 100);

        bar.value(-25);
        assertEquals(0, bar.value());
        assertEquals(0.0, bar.progress());

        bar.value(250);
        assertEquals(100, bar.value());
        assertEquals(1.0, bar.progress());
    }

    @Test
    void progressScalesWithinRange() {
        ProgressBarWidget bar = new ProgressBarWidget().range(20, 120).value(70);

        assertEquals(0.5, bar.progress(), 1e-9);
    }

    @Test
    void invalidRangeAndNonFiniteValuesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new ProgressBarWidget().range(5, 5));
        assertThrows(IllegalArgumentException.class, () -> new ProgressBarWidget().range(10, 5));
        assertThrows(IllegalArgumentException.class, () -> new ProgressBarWidget().value(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new ProgressBarWidget().value(Double.POSITIVE_INFINITY));
    }

    @Test
    void fillWidthTracksProgress() {
        Stack root = Stack.vertical();
        ProgressBarWidget bar = root.add(new ProgressBarWidget()
                .colors(0xFF101010, 0xFF00FF00, 0xFF555555, 0xFFFFFFFF)
                .fixedSize(102, 12)
                .value(0.5));
        WidgetHost host = new WidgetHost(root);
        host.resize(102, 12);
        RecordingRenderContext ctx = new RecordingRenderContext(102, 12);

        host.render(ctx);

        assertEquals(0.5, bar.progress(), 1e-9);
        assertTrue(ctx.ops.contains("fill(0,0,102,12,ff101010)"), "背景: " + ctx.ops);
        assertTrue(ctx.ops.contains("fill(1,1,50,10,ff00ff00)"),
                "内部填充宽度=round(0.5*100)=50: " + ctx.ops);
    }

    @Test
    void zeroProgressDrawsBackgroundButNoFill() {
        Stack root = Stack.vertical();
        root.add(new ProgressBarWidget()
                .colors(0xFF101010, 0xFF00FF00, 0xFF555555, 0xFFFFFFFF)
                .fixedSize(102, 12)
                .value(0.0));
        WidgetHost host = new WidgetHost(root);
        host.resize(102, 12);
        RecordingRenderContext ctx = new RecordingRenderContext(102, 12);

        host.render(ctx);

        assertTrue(ctx.ops.contains("fill(0,0,102,12,ff101010)"));
        assertFalse(ctx.ops.stream().anyMatch(op -> op.endsWith("ff00ff00)")), "0% 不绘制填充: " + ctx.ops);
    }

    @Test
    void labelFormatterRendersPercentText() {
        Stack root = Stack.vertical();
        ProgressBarWidget bar = root.add(new ProgressBarWidget()
                .fixedSize(102, 12)
                .label(progress -> Math.round(progress * 100) + "%")
                .value(0.5));
        WidgetHost host = new WidgetHost(root);
        host.resize(102, 12);
        RecordingRenderContext ctx = new RecordingRenderContext(102, 12);

        host.render(ctx);

        assertTrue(ctx.ops.stream().anyMatch(op -> op.startsWith("text(50%@")),
                "标签应显示 50%: " + ctx.ops);
        assertEquals(0.5, bar.progress(), 1e-9);
    }
}
