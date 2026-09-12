package com.gtsn.lib.ui.layout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 布局树节点：参数（{@link LayoutParams}）、子节点、可选固有内容测量器，
 * 以及布局引擎写入的结果（测量尺寸与绝对包围盒）。
 *
 * <p>本类为纯几何结构，不依赖 Minecraft；控件树（widget）持有各自的节点。</p>
 */
public final class LayoutNode {

    private final LayoutParams params;
    private final List<LayoutNode> children = new ArrayList<>();
    private ContentMeasurer contentMeasurer;
    private Size measuredSize = Size.ZERO;
    private Rect bounds = Rect.ZERO;
    private boolean measured;

    public LayoutNode() {
        this(new LayoutParams());
    }

    public LayoutNode(LayoutParams params) {
        this.params = Objects.requireNonNull(params, "params");
    }

    public LayoutParams params() {
        return params;
    }

    /** 只读子节点视图。 */
    public List<LayoutNode> children() {
        return Collections.unmodifiableList(children);
    }

    public LayoutNode addChild(LayoutNode child) {
        children.add(Objects.requireNonNull(child, "child"));
        return this;
    }

    /** 设置固有内容测量器（叶节点的自然尺寸来源），可空。 */
    public LayoutNode contentMeasurer(ContentMeasurer measurer) {
        this.contentMeasurer = measurer;
        return this;
    }

    public ContentMeasurer contentMeasurer() {
        return contentMeasurer;
    }

    public Size measuredSize() {
        return measuredSize;
    }

    /** 节点绝对包围盒（布局完成后有效）。 */
    public Rect bounds() {
        return bounds;
    }

    /** 节点内容区包围盒：包围盒向内扣除 padding。 */
    public Rect contentBounds() {
        return bounds.inset(params.padding());
    }

    public boolean isAbsolute() {
        return params.isAbsolute();
    }

    void setMeasuredSize(Size size) {
        this.measuredSize = Objects.requireNonNull(size, "size");
        this.measured = true;
    }

    /** 是否已被布局引擎测量过（绝对定位子节点在摆放阶段惰性测量时使用）。 */
    boolean isMeasured() {
        return measured;
    }

    void setBounds(Rect rect) {
        this.bounds = Objects.requireNonNull(rect, "rect");
    }
}
