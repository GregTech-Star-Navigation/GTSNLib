package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Anchor;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;

/**
 * 纯色盒控件：填充 + 可选边框，作为布局演示与容器背景的基础绘制单元。
 */
public final class BoxWidget extends AbstractWidget {

    private int fillColor = 0xFF202020;
    private int borderColor = 0x00000000;
    private int borderWidth = 1;

    public BoxWidget fill(int argb) {
        this.fillColor = argb;
        return this;
    }

    public BoxWidget border(int argb, int thickness) {
        this.borderColor = argb;
        this.borderWidth = Math.max(0, thickness);
        return this;
    }

    public BoxWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public BoxWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public BoxWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public BoxWidget padding(Insets padding) {
        node().params().padding(padding);
        return this;
    }

    public BoxWidget weight(float weight) {
        node().params().weight(weight);
        return this;
    }

    public BoxWidget absolute(Anchor anchor, int offsetX, int offsetY) {
        node().params().absolute(anchor, offsetX, offsetY);
        return this;
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        context.fill(box.x(), box.y(), box.width(), box.height(), fillColor);
        if (borderWidth > 0 && (borderColor >>> 24) != 0 && box.width() > 0 && box.height() > 0) {
            int thickness = Math.min(borderWidth, Math.min(box.width(), box.height()));
            context.fill(box.x(), box.y(), box.width(), thickness, borderColor);
            context.fill(box.x(), box.bottom() - thickness, box.width(), thickness, borderColor);
            context.fill(box.x(), box.y() + thickness, thickness, box.height() - 2 * thickness, borderColor);
            context.fill(box.right() - thickness, box.y() + thickness, thickness,
                    box.height() - 2 * thickness, borderColor);
        }
    }
}
