package com.gtsn.lib.ui.input;

import com.gtsn.lib.ui.widget.Widget;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 焦点管理：记录当前焦点控件与遍历顺序（Tab 换焦）。
 */
public final class FocusManager {

    private Widget focused;
    private List<Widget> order = List.of();

    public Optional<Widget> focusedWidget() {
        return Optional.ofNullable(focused);
    }

    public boolean isFocused(Widget widget) {
        return widget != null && widget == focused;
    }

    /** 请求聚焦；目标不可聚焦时返回 {@code false} 且焦点不变。传入 {@code null} 表示清除焦点。 */
    public boolean requestFocus(Widget widget) {
        if (widget != null && !widget.isFocusable()) {
            return false;
        }
        if (focused == widget) {
            return true;
        }
        Widget previous = focused;
        focused = widget;
        if (previous != null) {
            previous.onFocusChanged(false);
        }
        if (focused != null) {
            focused.onFocusChanged(true);
        }
        return true;
    }

    /** 清除焦点；原本无焦点时返回 {@code false}。 */
    public boolean clearFocus() {
        if (focused == null) {
            return false;
        }
        Widget previous = focused;
        focused = null;
        previous.onFocusChanged(false);
        return true;
    }

    /** 焦点移到顺序中的下一个；顺序为空返回 {@code false}。 */
    public boolean focusNext() {
        return move(1);
    }

    /** 焦点移到顺序中的上一个；顺序为空返回 {@code false}。 */
    public boolean focusPrevious() {
        return move(-1);
    }

    /** 更新遍历顺序（去重、保持出现顺序）；原焦点不在新顺序中时清除。 */
    void setFocusOrder(List<Widget> widgets) {
        Objects.requireNonNull(widgets, "widgets");
        LinkedHashSet<Widget> unique = new LinkedHashSet<>(widgets);
        unique.remove(null);
        order = List.copyOf(unique);
        if (focused != null && !order.contains(focused)) {
            clearFocus();
        }
    }

    private boolean move(int step) {
        if (order.isEmpty()) {
            return false;
        }
        int size = order.size();
        int index;
        if (focused == null) {
            index = step > 0 ? 0 : size - 1;
        } else {
            int current = order.indexOf(focused);
            index = current < 0 ? (step > 0 ? 0 : size - 1) : Math.floorMod(current + step, size);
        }
        return requestFocus(order.get(index));
    }
}
