package com.gtsn.lib.ui.layout;

/**
 * 栈布局方向。
 */
public enum Direction {
    VERTICAL,
    HORIZONTAL;

    /** 该方向上某尺寸的主轴分量。 */
    public int main(Size size) {
        return this == VERTICAL ? size.height() : size.width();
    }

    /** 该方向上某尺寸的交叉轴分量。 */
    public int cross(Size size) {
        return this == VERTICAL ? size.width() : size.height();
    }

    /** 该方向上某四边间距的主轴分量之和。 */
    public int main(Insets insets) {
        return this == VERTICAL ? insets.vertical() : insets.horizontal();
    }

    /** 该方向上某四边间距的交叉轴分量之和。 */
    public int cross(Insets insets) {
        return this == VERTICAL ? insets.horizontal() : insets.vertical();
    }
}
