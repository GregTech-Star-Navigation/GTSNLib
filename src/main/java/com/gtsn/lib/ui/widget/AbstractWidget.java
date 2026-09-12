package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.LayoutNode;
import com.gtsn.lib.ui.render.RenderContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 控件基类：持有布局节点与子控件列表，维护“子控件 ↔ 子节点”一一对应的树结构。
 *
 * <p>渲染顺序为 先自身（{@link #onRender}）后子控件（列表顺序，后添加者在上层）；
 * 命中测试由 {@code InputRouter} 按相反顺序处理。</p>
 */
public abstract class AbstractWidget implements Widget {

    private final LayoutNode node = new LayoutNode();
    private final List<Widget> children = new ArrayList<>();

    @Override
    public LayoutNode node() {
        return node;
    }

    @Override
    public List<Widget> children() {
        return Collections.unmodifiableList(children);
    }

    /** 挂接子控件并同步布局树，返回子控件便于链式构建。 */
    public <T extends Widget> T add(T child) {
        Objects.requireNonNull(child, "child");
        children.add(child);
        node.addChild(child.node());
        return child;
    }

    @Override
    public void render(RenderContext context) {
        Objects.requireNonNull(context, "context");
        onRender(context);
        for (Widget child : children) {
            child.render(context);
        }
    }

    /** 绘制自身（子控件由基类统一渲染）。 */
    protected void onRender(RenderContext context) {
    }
}
