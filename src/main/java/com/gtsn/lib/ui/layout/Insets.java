package com.gtsn.lib.ui.layout;

/**
 * 四边间距（像素），用于盒模型的 padding / margin。全部取值非负。
 */
public record Insets(int top, int right, int bottom, int left) {

    public static final Insets NONE = new Insets(0, 0, 0, 0);

    public Insets {
        if (top < 0 || right < 0 || bottom < 0 || left < 0) {
            throw new IllegalArgumentException(
                    "insets must be non-negative: top=" + top + " right=" + right + " bottom=" + bottom + " left=" + left);
        }
    }

    public static Insets all(int value) {
        return new Insets(value, value, value, value);
    }

    public static Insets symmetric(int vertical, int horizontal) {
        return new Insets(vertical, horizontal, vertical, horizontal);
    }

    public int horizontal() {
        return left + right;
    }

    public int vertical() {
        return top + bottom;
    }
}
