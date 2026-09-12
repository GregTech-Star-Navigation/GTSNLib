package com.gtsn.lib.ui.layout;

/**
 * 布局参数：节点的盒模型（padding/margin）、双轴尺寸策略、主轴权重，
 * 以及作为容器时的栈方向 / 对齐 / 间距；或作为绝对定位节点的锚点。
 *
 * <p>可变流式 API，构造一次后由布局引擎只读消费。</p>
 */
public final class LayoutParams {

    private Sizing width = Sizing.WRAP;
    private Sizing height = Sizing.WRAP;
    private Insets margin = Insets.NONE;
    private Insets padding = Insets.NONE;
    private int gap;
    private Direction direction = Direction.VERTICAL;
    private MainAxisAlign mainAxisAlign = MainAxisAlign.START;
    private CrossAxisAlign crossAxisAlign = CrossAxisAlign.START;
    private float weight;
    private boolean absolute;
    private Anchor anchor = Anchor.TOP_LEFT;
    private int anchorOffsetX;
    private int anchorOffsetY;

    public static LayoutParams create() {
        return new LayoutParams();
    }

    /** 双轴固定像素的快捷参数。 */
    public static LayoutParams fixedSize(int width, int height) {
        return create().width(Sizing.fixed(width)).height(Sizing.fixed(height));
    }

    public Sizing width() {
        return width;
    }

    public LayoutParams width(Sizing sizing) {
        this.width = requireSizing(sizing);
        return this;
    }

    public Sizing height() {
        return height;
    }

    public LayoutParams height(Sizing sizing) {
        this.height = requireSizing(sizing);
        return this;
    }

    public LayoutParams size(Sizing width, Sizing height) {
        return width(width).height(height);
    }

    public LayoutParams fillWidth() {
        return width(Sizing.FILL);
    }

    public LayoutParams fillHeight() {
        return height(Sizing.FILL);
    }

    public LayoutParams fill() {
        return fillWidth().fillHeight();
    }

    public Insets margin() {
        return margin;
    }

    public LayoutParams margin(Insets margin) {
        this.margin = requireInsets(margin);
        return this;
    }

    public Insets padding() {
        return padding;
    }

    public LayoutParams padding(Insets padding) {
        this.padding = requireInsets(padding);
        return this;
    }

    public int gap() {
        return gap;
    }

    public LayoutParams gap(int gap) {
        if (gap < 0) {
            throw new IllegalArgumentException("gap must be non-negative: " + gap);
        }
        this.gap = gap;
        return this;
    }

    public Direction direction() {
        return direction;
    }

    public LayoutParams direction(Direction direction) {
        this.direction = java.util.Objects.requireNonNull(direction, "direction");
        return this;
    }

    public MainAxisAlign mainAxisAlign() {
        return mainAxisAlign;
    }

    public LayoutParams mainAxisAlign(MainAxisAlign align) {
        this.mainAxisAlign = java.util.Objects.requireNonNull(align, "align");
        return this;
    }

    public CrossAxisAlign crossAxisAlign() {
        return crossAxisAlign;
    }

    public LayoutParams crossAxisAlign(CrossAxisAlign align) {
        this.crossAxisAlign = java.util.Objects.requireNonNull(align, "align");
        return this;
    }

    public float weight() {
        return weight;
    }

    public LayoutParams weight(float weight) {
        if (weight < 0 || Float.isNaN(weight) || Float.isInfinite(weight)) {
            throw new IllegalArgumentException("weight must be finite and non-negative: " + weight);
        }
        this.weight = weight;
        return this;
    }

    /** 绝对定位：脱离栈流，由锚点相对父容器内容区放置。 */
    public boolean isAbsolute() {
        return absolute;
    }

    public Anchor anchor() {
        return anchor;
    }

    public int anchorOffsetX() {
        return anchorOffsetX;
    }

    public int anchorOffsetY() {
        return anchorOffsetY;
    }

    public LayoutParams absolute(Anchor anchor) {
        return absolute(anchor, 0, 0);
    }

    public LayoutParams absolute(Anchor anchor, int offsetX, int offsetY) {
        this.absolute = true;
        this.anchor = java.util.Objects.requireNonNull(anchor, "anchor");
        this.anchorOffsetX = offsetX;
        this.anchorOffsetY = offsetY;
        return this;
    }

    private static Sizing requireSizing(Sizing sizing) {
        return java.util.Objects.requireNonNull(sizing, "sizing");
    }

    private static Insets requireInsets(Insets insets) {
        return java.util.Objects.requireNonNull(insets, "insets");
    }
}
