package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;

/**
 * 裁剪容器：子控件渲染与命中测试都被限制在自身包围盒内（滚动视口/溢出示意的基元）。
 */
public final class ClipWidget extends AbstractWidget {

    public ClipWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public ClipWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public ClipWidget padding(Insets padding) {
        node().params().padding(padding);
        return this;
    }

    public ClipWidget weight(float weight) {
        node().params().weight(weight);
        return this;
    }

    @Override
    public boolean clipsChildren() {
        return true;
    }

    @Override
    public void render(RenderContext context) {
        Rect box = bounds();
        context.pushClip(box.x(), box.y(), box.width(), box.height());
        super.render(context);
        context.popClip();
    }
}
