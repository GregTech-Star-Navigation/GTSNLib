package com.gtsn.lib.ui.input;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 焦点管理行为：可聚焦校验、幂等聚焦、清除、顺序循环与顺序重建。
 */
class FocusManagerTest {

    private final List<String> log = new ArrayList<>();
    private final FocusManager focus = new FocusManager();

    private ProbeWidget focusable(String name) {
        return new ProbeWidget(name, log).fixed(10, 10).focusable();
    }

    @Test
    void requestFocusRejectsNonFocusableWidget() {
        ProbeWidget plain = new ProbeWidget("plain", log).fixed(10, 10);

        assertFalse(focus.requestFocus(plain));
        assertTrue(focus.focusedWidget().isEmpty());
    }

    @Test
    void requestFocusIsIdempotentAndClearNotifiesOnce() {
        ProbeWidget a = focusable("a");

        assertTrue(focus.requestFocus(a));
        assertTrue(focus.requestFocus(a), "重复聚焦返回 true 但不重复通知");
        assertTrue(focus.isFocused(a));
        assertTrue(log.stream().filter(line -> line.equals("a:focus=true")).count() == 1, "只通知一次: " + log);

        assertTrue(focus.clearFocus());
        assertFalse(focus.isFocused(a));
        assertTrue(log.contains("a:focus=false"));
        assertFalse(focus.clearFocus(), "无焦点时清除返回 false");
    }

    @Test
    void focusOrderCyclesBothDirectionsWithWraparound() {
        ProbeWidget a = focusable("a");
        ProbeWidget b = focusable("b");
        ProbeWidget c = focusable("c");
        focus.setFocusOrder(List.of(a, b, c));

        assertTrue(focus.focusNext());
        assertTrue(focus.isFocused(a));
        assertTrue(focus.focusNext());
        assertTrue(focus.isFocused(b));
        assertTrue(focus.focusPrevious());
        assertTrue(focus.isFocused(a));
        assertTrue(focus.focusPrevious());
        assertTrue(focus.isFocused(c), "反向回绕到末尾");
    }

    @Test
    void rebuildingOrderDropsStaleFocus() {
        ProbeWidget a = focusable("a");
        ProbeWidget b = focusable("b");
        ProbeWidget c = focusable("c");
        focus.setFocusOrder(List.of(a, b, c));
        focus.requestFocus(b);

        focus.setFocusOrder(List.of(a, c));

        assertTrue(focus.focusedWidget().isEmpty(), "焦点不在新顺序中时清除");
        assertTrue(log.contains("b:focus=false"));

        assertTrue(focus.focusNext(), "重建后的顺序仍可换焦");
        assertTrue(focus.isFocused(a));
    }
}
