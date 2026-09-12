package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.input.Keys;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.screen.WidgetHost;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物品槽：槽底绘制、图标绘制区、悬停/按下/禁用状态、可选选择态与点击回调。
 */
class ItemSlotWidgetTest {

    private ItemSlotWidget slot;
    private WidgetHost host;

    @BeforeEach
    void setUp() {
        Stack root = Stack.vertical();
        slot = root.add(new ItemSlotWidget().fixedSize(18, 18));
        host = new WidgetHost(root);
        host.resize(40, 40);
    }

    private void click(double x, double y) {
        host.dispatch(new InputEvent.MousePressed(x, y, 0));
        host.dispatch(new InputEvent.MouseReleased(x, y, 0));
    }

    @Test
    void slotPaintsFrameThenIconWithinInsetArea() {
        AtomicReference<Rect> painted = new AtomicReference<>();
        slot.icon((context, bounds) -> {
            painted.set(bounds);
            context.fill(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF00FF00);
        });
        RecordingRenderContext ctx = new RecordingRenderContext(40, 40);

        host.render(ctx);

        assertEquals(Rect.of(1, 1, 16, 16), painted.get(), "图标绘制区为槽内缩 1px");
        assertEquals(Rect.of(1, 1, 16, 16), slot.iconBounds());
        assertEquals("fill(1,1,16,16,ff00ff00)", ctx.ops.get(5), "槽底 5 次填充后绘制图标: " + ctx.ops);
    }

    @Test
    void emptySlotReportsEmptyWithoutIcon() {
        assertTrue(slot.isEmpty());
        slot.icon((context, bounds) -> {
        });
        assertFalse(slot.isEmpty());
    }

    @Test
    void selectableSlotTogglesSelectionOnClickAndKeyboard() {
        List<Boolean> changes = new ArrayList<>();
        slot.selectable(true).onSelectionChanged(changes::add);

        assertTrue(slot.isFocusable());
        click(9, 9);
        assertTrue(slot.isSelected());
        assertEquals(List.of(true), changes);

        host.router().focus().requestFocus(slot);
        assertTrue(host.dispatch(new InputEvent.KeyPressed(Keys.SPACE, 0, 0)));
        assertFalse(slot.isSelected());
    }

    @Test
    void nonInteractiveSlotIsNotFocusableAndIgnoresPress() {
        assertFalse(slot.isFocusable());
        assertFalse(host.dispatch(new InputEvent.MousePressed(9, 9, 0)), "纯展示槽不消费按下事件");
        assertFalse(slot.isPressed());
    }

    @Test
    void displaySlotFiresOnClickWithoutSelectionToggle() {
        AtomicInteger clicks = new AtomicInteger();
        slot.onClick(clicks::incrementAndGet);

        click(9, 9);

        assertEquals(1, clicks.get());
        assertFalse(slot.isSelected());
    }

    @Test
    void hoverPressAndDisabledStatesAreTracked() {
        slot.selectable(true);

        host.dispatch(new InputEvent.MouseMoved(9, 9));
        assertTrue(slot.isHovered());

        host.dispatch(new InputEvent.MousePressed(9, 9, 0));
        assertTrue(slot.isPressed());

        host.dispatch(new InputEvent.MouseReleased(9, 9, 0));
        assertFalse(slot.isPressed());

        slot.enabled(false);
        assertFalse(slot.isFocusable());
        boolean selectionBefore = slot.isSelected();
        click(9, 9);
        assertEquals(selectionBefore, slot.isSelected(), "禁用后点击不改变选择态");
    }

    @Test
    void selectedSlotRendersAdditionalBorder() {
        slot.selectable(true);
        RecordingRenderContext before = new RecordingRenderContext(40, 40);
        host.render(before);
        List<String> plain = List.copyOf(before.ops);

        slot.selected(true);
        before.ops.clear();
        host.render(before);

        assertNotEquals(plain, List.copyOf(before.ops), "选中态应绘制额外高亮边框");
    }
}
