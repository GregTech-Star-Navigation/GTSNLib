package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;

/**
 * 分隔线：水平或垂直的单色线，用于面板内分组。
 *
 * <p>水平线横向填满、纵向固定厚度；垂直线相反。颜色默认中性灰。</p>
 */
public final class DividerWidget extends AbstractWidget {

    private boolean horizontal = true;
    private int color = 0xFF4A4A4A;
    private int thickness = 1;

    public DividerWidget() {
        applySize();
    }

    public static DividerWidget horizontal() {
        return new DividerWidget();
    }

    public static DividerWidget vertical() {
        DividerWidget divider = new DividerWidget();
        divider.horizontal = false;
        divider.applySize();
        return divider;
    }

    public DividerWidget color(int argb) {
        this.color = argb;
        return this;
    }

    public int color() {
        return color;
    }

    public DividerWidget thickness(int pixels) {
        if (pixels < 1) {
            throw new IllegalArgumentException("thickness must be >= 1: " + pixels);
        }
        this.thickness = pixels;
        applySize();
        return this;
    }

    public int thickness() {
        return thickness;
    }

    public boolean isHorizontal() {
        return horizontal;
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        context.fill(box.x(), box.y(), box.width(), box.height(), color);
    }

    private void applySize() {
        if (horizontal) {
            node().params().size(Sizing.fill(), Sizing.fixed(thickness));
        } else {
            node().params().size(Sizing.fixed(thickness), Sizing.fill());
        }
    }
}
