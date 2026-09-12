package com.gtsn.lib.ui.screen;

import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.input.InputRouter;
import com.gtsn.lib.ui.layout.LayoutEngine;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.widget.Widget;

import java.util.Objects;

/**
 * 控件泊点：持有一棵控件树、驱动布局（resize）、渲染与输入派发。
 *
 * <p>本类不依赖 Minecraft，客户端的 {@code GtsnScreen} 只是它的薄壳（转发生命周期回调与坐标）。</p>
 */
public final class WidgetHost {

    private final Widget root;
    private final InputRouter router;
    private int width;
    private int height;

    public WidgetHost(Widget root) {
        this.root = Objects.requireNonNull(root, "root");
        this.router = new InputRouter(root);
    }

    public Widget root() {
        return root;
    }

    public InputRouter router() {
        return router;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** 按屏幕尺寸重新布局整棵树（根节点包围盒即屏幕）。 */
    public void resize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("host size must be non-negative: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        LayoutEngine.layout(root.node(), Rect.of(0, 0, width, height));
    }

    /** 渲染整棵控件树。 */
    public void render(RenderContext context) {
        root.render(Objects.requireNonNull(context, "context"));
    }

    /** 派发输入事件；返回是否被消费。 */
    public boolean dispatch(InputEvent event) {
        return router.dispatch(Objects.requireNonNull(event, "event"));
    }
}
