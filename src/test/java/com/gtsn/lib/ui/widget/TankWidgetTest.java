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
 * 流体罐：存量/容量钳制、比例映射、自底向上的填充高度。
 */
class TankWidgetTest {

    @BeforeAll
    static void bootstrapRegistries() {
        MinecraftTestBootstrap.ensure();
    }

    @Test
    void defaultStateIsEmpty() {
        TankWidget tank = new TankWidget();

        assertEquals(0L, tank.amount());
        assertEquals(0L, tank.capacity());
        assertEquals(0.0, tank.ratio());
        assertTrue(tank.isEmpty());
        assertEquals("", tank.fluidName());
    }

    @Test
    void amountAndNameAreStored() {
        TankWidget tank = new TankWidget().tank(600, 1000).fluidName("水");

        assertEquals(600L, tank.amount());
        assertEquals(1000L, tank.capacity());
        assertEquals(0.6, tank.ratio(), 1e-9);
        assertFalse(tank.isEmpty());
        assertEquals("水", tank.fluidName());
    }

    @Test
    void amountClampsToCapacityAndNormalizesNegatives() {
        assertEquals(1000L, new TankWidget().tank(1500, 1000).amount());
        assertEquals(0L, new TankWidget().tank(-20, 1000).amount());
        assertEquals(0.0, new TankWidget().tank(20, 0).ratio(), 1e-9);
    }

    @Test
    void fillRisesFromBottom() {
        Stack root = Stack.vertical();
        TankWidget tank = root.add(new TankWidget()
                .colors(0xFF101010, 0xFF0088FF, 0xFF555555)
                .fixedSize(20, 44)
                .tank(500, 1000));
        WidgetHost host = new WidgetHost(root);
        host.resize(20, 44);
        RecordingRenderContext ctx = new RecordingRenderContext(20, 44);

        host.render(ctx);

        assertEquals(0.5, tank.ratio(), 1e-9);
        assertTrue(ctx.ops.contains("fill(0,0,20,44,ff101010)"), "罐底: " + ctx.ops);
        assertTrue(ctx.ops.contains("fill(1,22,18,21,ff0088ff)"), "50% 自底部填 21px: " + ctx.ops);
    }
}
