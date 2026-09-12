package com.gtsn.lib.ui.layout;

/**
 * 轴对齐矩形（像素坐标）。{@link #contains(double, double)} 采用左上闭、右下开区间，
 * 保证相邻矩形在共享边界上不会同时命中同一点。
 */
public record Rect(int x, int y, int width, int height) {

    public static final Rect ZERO = new Rect(0, 0, 0, 0);

    public Rect {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("rect must be non-negative: " + width + "x" + height);
        }
    }

    public static Rect of(int x, int y, int width, int height) {
        return new Rect(x, y, width, height);
    }

    public int right() {
        return x + width;
    }

    public int bottom() {
        return y + height;
    }

    public boolean isEmpty() {
        return width == 0 || height == 0;
    }

    /** 左上闭、右下开区间命中测试。 */
    public boolean contains(double px, double py) {
        return px >= x && px < right() && py >= y && py < bottom();
    }

    /** 向内收缩四边；收缩量超出尺寸时结果退化为 0（不抛异常）。 */
    public Rect inset(Insets insets) {
        return new Rect(
                x + insets.left(),
                y + insets.top(),
                Math.max(0, width - insets.horizontal()),
                Math.max(0, height - insets.vertical()));
    }

    public Rect offset(int dx, int dy) {
        return new Rect(x + dx, y + dy, width, height);
    }

    public boolean intersects(Rect other) {
        return x < other.right() && other.x < right() && y < other.bottom() && other.y < bottom();
    }

    /**
     * 交集；无重叠时返回空心矩形（宽高为 0，左上角取两矩形左上角的较大者）。
     */
    public Rect intersect(Rect other) {
        int ix = Math.max(x, other.x);
        int iy = Math.max(y, other.y);
        int ir = Math.min(right(), other.right());
        int ib = Math.min(bottom(), other.bottom());
        if (ir <= ix || ib <= iy) {
            return new Rect(ix, iy, 0, 0);
        }
        return new Rect(ix, iy, ir - ix, ib - iy);
    }

    public Size size() {
        return new Size(width, height);
    }
}
