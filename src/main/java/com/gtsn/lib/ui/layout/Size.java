package com.gtsn.lib.ui.layout;

/**
 * 二维整数尺寸（像素）。布局引擎与几何计算的基础值类型，不依赖 Minecraft。
 */
public record Size(int width, int height) {

    public static final Size ZERO = new Size(0, 0);

    public Size {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("size must be non-negative: " + width + "x" + height);
        }
    }

    public static Size of(int width, int height) {
        return new Size(width, height);
    }

    public boolean isEmpty() {
        return width == 0 || height == 0;
    }
}
