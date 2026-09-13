package com.gtsn.lib.ui.widget;

import com.gtsn.lib.testing.MinecraftTestBootstrap;
import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.screen.WidgetHost;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 进度箭头：进度/总量钳制、工作态、按比例裁剪的箭头填充。
 */
class ProgressArrowWidgetTest {

    @BeforeAll
    static void bootstrapRegistries() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    void defaultStateIsIdle() {
        ProgressArrowWidget arrow = new ProgressArrowWidget();

        assertEquals(0, arrow.progress());
        assertEquals(0, arrow.maxProgress());
        assertEquals(0.0, arrow.ratio());
        assertFalse(arrow.working());
    }

    @Test
    void progressClampsToUnitInterval() {
        assertEquals(0.3, new ProgressArrowWidget().progress(30, 100).ratio(), 1e-9);
        assertEquals(1.0, new ProgressArrowWidget().progress(200, 100).ratio(), 1e-9);
        assertEquals(0.0, new ProgressArrowWidget().progress(30, 0).ratio(), 1e-9);
        assertEquals(0, new ProgressArrowWidget().progress(-5, 100).progress());
    }

    @Test
    void workingFlagIsTracked() {
        ProgressArrowWidget arrow = new ProgressArrowWidget().progress(30, 100).working(true);

        assertTrue(arrow.working());
        assertEquals(0.3, arrow.ratio(), 1e-9);
    }

    @Test
    void fillIsClippedToRatioWidth() {
        Stack root = Stack.vertical();
        ProgressArrowWidget arrow = root.add(new ProgressArrowWidget()
                .colors(0xFF101010, 0xFF00A0FF, 0xFF555555)
                .fixedSize(40, 16)
                .progress(20, 100));
        WidgetHost host = new WidgetHost(root);
        host.resize(40, 16);
        RecordingRenderContext ctx = new RecordingRenderContext(40, 16);

        host.render(ctx);

        assertEquals(0.2, arrow.ratio(), 1e-9);
        assertTrue(ctx.ops.contains("fill(0,0,40,16,ff101010)"), "轨道: " + ctx.ops);
        assertTrue(ctx.ops.contains("pushClip(0,0,8,16)"), "按比例 8px 裁剪: " + ctx.ops);
        assertTrue(ctx.ops.contains("popClip"), "裁剪必须闭合: " + ctx.ops);
    }

    /**
     * 工作态真的影响渲染（#22 评审 F1-6）：空闲（默认 false）把填充色 alpha 减半，运行中用完整填充色。
     */
    @Test
    void idleFillIsDimmedWhileWorkingFillIsFull() {
        RecordingRenderContext idle = renderArrow(false);
        RecordingRenderContext working = renderArrow(true);

        assertTrue(idle.ops.stream().anyMatch(op -> op.endsWith(",8000a0ff)")),
                "空闲态应为半透明填充: " + idle.ops);
        assertFalse(idle.ops.stream().anyMatch(op -> op.endsWith(",ff00a0ff)")), "空闲态不应整色填充: " + idle.ops);

        assertTrue(working.ops.stream().anyMatch(op -> op.endsWith(",ff00a0ff)")),
                "工作态应为完整填充色: " + working.ops);
    }

    private static RecordingRenderContext renderArrow(boolean working) {
        Stack root = Stack.vertical();
        ProgressArrowWidget arrow = root.add(new ProgressArrowWidget()
                .colors(0xFF101010, 0xFF00A0FF, 0xFF555555)
                .working(working)
                .fixedSize(40, 16)
                .progress(20, 100));
        WidgetHost host = new WidgetHost(root);
        host.resize(40, 16);
        RecordingRenderContext ctx = new RecordingRenderContext(40, 16);
        host.render(ctx);
        assertEquals(0.2, arrow.ratio(), 1e-9);
        return ctx;
    }
}
