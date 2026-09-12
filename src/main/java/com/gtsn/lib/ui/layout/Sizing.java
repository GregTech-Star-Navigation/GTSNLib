package com.gtsn.lib.ui.layout;

/**
 * 单轴尺寸策略：固定像素 / 包裹内容 / 填满可用空间。
 */
public sealed interface Sizing {

    /** 固定像素（不含 margin）。 */
    record Fixed(int pixels) implements Sizing {
        public Fixed {
            if (pixels < 0) {
                throw new IllegalArgumentException("fixed size must be non-negative: " + pixels);
            }
        }
    }

    /** 包裹内容：尺寸由 padding + 子节点/固有内容决定。 */
    record Wrap() implements Sizing {
    }

    /** 填满父容器该轴可用空间；在主轴（栈方向）上与 weight=1 等价。 */
    record Fill() implements Sizing {
    }

    Sizing WRAP = new Wrap();
    Sizing FILL = new Fill();

    static Sizing fixed(int pixels) {
        return new Fixed(pixels);
    }

    static Sizing wrap() {
        return WRAP;
    }

    static Sizing fill() {
        return FILL;
    }
}
