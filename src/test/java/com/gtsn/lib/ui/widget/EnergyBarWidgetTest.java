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
 * 能量条：存量/容量钳制、比例映射、填充宽度与内置标签文本。
 */
class EnergyBarWidgetTest {

    @BeforeAll
    static void bootstrapRegistries() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    void defaultStateIsEmpty() {
        EnergyBarWidget bar = new EnergyBarWidget();

        assertEquals(0L, bar.stored());
        assertEquals(0L, bar.capacity());
        assertEquals(0.0, bar.ratio());
        assertFalse(bar.hasEnergy());
    }

    @Test
    void energyStoredClampsToCapacity() {
        EnergyBarWidget bar = new EnergyBarWidget().energy(2000, 1000);

        assertEquals(1000L, bar.stored());
        assertEquals(1000L, bar.capacity());
        assertEquals(1.0, bar.ratio(), 1e-9);
        assertTrue(bar.hasEnergy());
    }

    @Test
    void negativeAndZeroCapacityNormalizeSafely() {
        assertEquals(0L, new EnergyBarWidget().energy(-50, 100).stored());
        assertEquals(0.0, new EnergyBarWidget().energy(-50, 100).ratio(), 1e-9);
        EnergyBarWidget noCapacity = new EnergyBarWidget().energy(50, 0);
        assertEquals(0L, noCapacity.stored());
        assertEquals(0.0, noCapacity.ratio(), 1e-9);
    }

    @Test
    void labelTextReportsStoredOverCapacity() {
        EnergyBarWidget bar = new EnergyBarWidget().energy(200, 1000);

        assertEquals("200/1000 EU", bar.labelText());
        assertEquals("0/0 EU", new EnergyBarWidget().labelText());
    }

    @Test
    void fillWidthTracksRatio() {
        Stack root = Stack.vertical();
        EnergyBarWidget bar = root.add(new EnergyBarWidget()
                .colors(0xFF101010, 0xFF00FF00, 0xFF20FF20, 0xFF555555, 0xFFFFFFFF)
                .fixedSize(102, 12)
                .energy(500, 1000));
        WidgetHost host = new WidgetHost(root);
        host.resize(102, 12);
        RecordingRenderContext ctx = new RecordingRenderContext(102, 12);

        host.render(ctx);

        assertEquals(0.5, bar.ratio(), 1e-9);
        assertTrue(ctx.ops.contains("fill(0,0,102,12,ff101010)"), "背景: " + ctx.ops);
        assertTrue(ctx.ops.contains("fill(1,1,50,10,ff00ff00)"), "填充宽度=50: " + ctx.ops);
    }
}
