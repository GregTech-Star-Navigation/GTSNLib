package com.gtsn.lib.ui.layout;

import java.util.Objects;

/**
 * 单轴测量约束，三态语义对齐 Android MeasureSpec：
 * {@link Mode#EXACTLY} 强制给定尺寸、{@link Mode#AT_MOST} 上限约束、{@link Mode#UNSPECIFIED} 不约束。
 */
public record MeasureSpec(int size, Mode mode) {

    public enum Mode {
        EXACTLY,
        AT_MOST,
        UNSPECIFIED
    }

    public static final MeasureSpec UNSPECIFIED = new MeasureSpec(0, Mode.UNSPECIFIED);

    public MeasureSpec {
        Objects.requireNonNull(mode, "mode");
        if (size < 0) {
            throw new IllegalArgumentException("spec size must be non-negative: " + size);
        }
    }

    public static MeasureSpec exactly(int size) {
        return new MeasureSpec(size, Mode.EXACTLY);
    }

    public static MeasureSpec atMost(int maxSize) {
        return new MeasureSpec(maxSize, Mode.AT_MOST);
    }

    public static MeasureSpec unspecified() {
        return UNSPECIFIED;
    }

    public boolean isBounded() {
        return mode != Mode.UNSPECIFIED;
    }

    /** 把期望尺寸解析为实际尺寸：EXACTLY 取约束值，AT_MOST 取较小者，UNSPECIFIED 取期望值。 */
    public int resolve(int desired) {
        int clamped = Math.max(0, desired);
        return switch (mode) {
            case EXACTLY -> size;
            case AT_MOST -> Math.min(size, clamped);
            case UNSPECIFIED -> clamped;
        };
    }

    /** 扣除固定占位（如 padding）后的同模式约束；UNSPECIFIED 保持不变。 */
    public MeasureSpec subtract(int amount) {
        if (mode == Mode.UNSPECIFIED) {
            return this;
        }
        return new MeasureSpec(Math.max(0, size - amount), mode);
    }
}
