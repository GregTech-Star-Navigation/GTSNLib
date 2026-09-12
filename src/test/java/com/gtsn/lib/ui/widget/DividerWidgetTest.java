package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.screen.WidgetHost;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分隔线：水平/垂直两向、厚度与颜色，作为面板内分组基元。
 */
class DividerWidgetTest {

    @Test
    void horizontalDividerFillsWidthAndKeepsThickness() {
        Stack root = Stack.vertical().padding(Insets.all(2));
        DividerWidget divider = root.add(DividerWidget.horizontal().thickness(2).color(0xFF446688));
        WidgetHost host = new WidgetHost(root);
        host.resize(60, 30);
        RecordingRenderContext ctx = new RecordingRenderContext(60, 30);

        host.render(ctx);

        assertEquals(Rect.of(2, 2, 56, 2), divider.bounds());
        assertEquals("fill(2,2,56,2,ff446688)", ctx.ops.get(0));
    }

    @Test
    void verticalDividerFillsHeightAndKeepsThickness() {
        Stack root = Stack.horizontal().padding(Insets.all(2));
        DividerWidget divider = root.add(DividerWidget.vertical().thickness(3));
        WidgetHost host = new WidgetHost(root);
        host.resize(60, 30);

        assertEquals(Rect.of(2, 2, 3, 26), divider.bounds());
    }

    @Test
    void dividerDefaultsToOnePixel() {
        Stack root = Stack.vertical();
        DividerWidget divider = root.add(DividerWidget.horizontal());
        WidgetHost host = new WidgetHost(root);
        host.resize(40, 10);

        assertEquals(1, divider.bounds().height());
        assertEquals(40, divider.bounds().width());
        assertTrue(divider.color() != 0, "默认颜色必须可见");
    }
}
