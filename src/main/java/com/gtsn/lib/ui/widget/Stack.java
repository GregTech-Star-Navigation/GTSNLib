package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Anchor;
import com.gtsn.lib.ui.layout.CrossAxisAlign;
import com.gtsn.lib.ui.layout.Direction;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.MainAxisAlign;
import com.gtsn.lib.ui.layout.Sizing;

/**
 * 栈容器控件：按方向排列子控件的便捷构建器（参数落在自身 {@link com.gtsn.lib.ui.layout.LayoutParams}）。
 */
public final class Stack extends AbstractWidget {

    public Stack() {
    }

    public Stack(Direction direction) {
        node().params().direction(direction);
    }

    public static Stack vertical() {
        return new Stack(Direction.VERTICAL);
    }

    public static Stack horizontal() {
        return new Stack(Direction.HORIZONTAL);
    }

    public Stack gap(int gap) {
        node().params().gap(gap);
        return this;
    }

    public Stack padding(Insets padding) {
        node().params().padding(padding);
        return this;
    }

    public Stack margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public Stack align(MainAxisAlign main, CrossAxisAlign cross) {
        node().params().mainAxisAlign(main).crossAxisAlign(cross);
        return this;
    }

    public Stack mainAxisAlign(MainAxisAlign align) {
        node().params().mainAxisAlign(align);
        return this;
    }

    public Stack crossAxisAlign(CrossAxisAlign align) {
        node().params().crossAxisAlign(align);
        return this;
    }

    public Stack size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public Stack fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public Stack fill() {
        node().params().fill();
        return this;
    }

    public Stack fillWidth() {
        node().params().fillWidth();
        return this;
    }

    public Stack fillHeight() {
        node().params().fillHeight();
        return this;
    }

    public Stack weight(float weight) {
        node().params().weight(weight);
        return this;
    }

    public Stack absolute(Anchor anchor, int offsetX, int offsetY) {
        node().params().absolute(anchor, offsetX, offsetY);
        return this;
    }
}
