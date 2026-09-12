package com.gtsn.lib.ui.sync;

import java.util.Objects;

/**
 * 内置编解码器工厂：int / 区间 int、float / 区间 float、bool、枚举与索引助手。
 *
 * <p>钳制与兜底规则：写侧（{@link SyncCodec#encode}）拒绝 null 与非有限浮点，越界值钳制到范围；
 * 读侧（{@link SyncCodec#decode}）面对任意原始值兜底——越界钳制、非法浮点取兜底常量、非法枚举序数
 * 回退到首个常量（或指定常量）。</p>
 */
public final class SyncCodecs {

    private SyncCodecs() {
    }

    /** 任意 int。 */
    public static SyncCodec<Integer> ints() {
        return new SyncCodec<>() {
            @Override
            public int encode(Integer value) {
                return Objects.requireNonNull(value, "value");
            }

            @Override
            public Integer decode(int raw) {
                return raw;
            }
        };
    }

    /** 区间 int：编解码均钳制到 {@code [min, max]}。 */
    public static SyncCodec<Integer> intRange(int min, int max) {
        if (max <= min) {
            throw new IllegalArgumentException("invalid int range: min=" + min + " max=" + max);
        }
        return new SyncCodec<>() {
            @Override
            public int encode(Integer value) {
                return clamp(Objects.requireNonNull(value, "value"), min, max);
            }

            @Override
            public Integer decode(int raw) {
                return clamp(raw, min, max);
            }
        };
    }

    /** 任意有限 float（按原始位精确往返；非有限值编码拒绝、解码兜底为 {@code 0}）。 */
    public static SyncCodec<Float> floats() {
        return new SyncCodec<>() {
            @Override
            public int encode(Float value) {
                return Float.floatToRawIntBits(requireFinite(value));
            }

            @Override
            public Float decode(int raw) {
                float value = Float.intBitsToFloat(raw);
                return Float.isFinite(value) ? value : 0f;
            }
        };
    }

    /** 区间 float：编解码均钳制到 {@code [min, max]}；非有限值编码拒绝、解码兜底为 {@code min}。 */
    public static SyncCodec<Float> floatRange(float min, float max) {
        if (!Float.isFinite(min) || !Float.isFinite(max) || max <= min) {
            throw new IllegalArgumentException("invalid float range: min=" + min + " max=" + max);
        }
        return new SyncCodec<>() {
            @Override
            public int encode(Float value) {
                return Float.floatToRawIntBits(clamp(requireFinite(value), min, max));
            }

            @Override
            public Float decode(int raw) {
                float value = Float.intBitsToFloat(raw);
                return Float.isFinite(value) ? clamp(value, min, max) : min;
            }
        };
    }

    /** 布尔（1 = true，其余为 false）。 */
    public static SyncCodec<Boolean> bools() {
        return new SyncCodec<>() {
            @Override
            public int encode(Boolean value) {
                return Objects.requireNonNull(value, "value") ? 1 : 0;
            }

            @Override
            public Boolean decode(int raw) {
                return raw != 0;
            }
        };
    }

    /** 枚举（序数编码；越界解码回退到首个常量）。 */
    public static <E extends Enum<E>> SyncCodec<E> enums(Class<E> enumType) {
        E[] constants = requireEnumConstants(enumType);
        return enums(enumType, constants[0]);
    }

    /** 枚举（序数编码；越界解码回退到指定常量）。 */
    public static <E extends Enum<E>> SyncCodec<E> enums(Class<E> enumType, E fallback) {
        E[] constants = requireEnumConstants(enumType);
        E safeFallback = Objects.requireNonNull(fallback, "fallback");
        return new SyncCodec<>() {
            @Override
            public int encode(E value) {
                return Objects.requireNonNull(value, "value").ordinal();
            }

            @Override
            public E decode(int raw) {
                return raw >= 0 && raw < constants.length ? constants[raw] : safeFallback;
            }
        };
    }

    /** 索引助手：合法值 {@code [0, size)}，编解码均钳制。 */
    public static SyncCodec<Integer> index(int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("index size must be positive: " + size);
        }
        return intRange(0, size - 1);
    }

    private static <E extends Enum<E>> E[] requireEnumConstants(Class<E> enumType) {
        Objects.requireNonNull(enumType, "enumType");
        E[] constants = enumType.getEnumConstants();
        if (constants == null || constants.length == 0) {
            throw new IllegalArgumentException("not an enum type: " + enumType);
        }
        return constants;
    }

    private static float requireFinite(Float value) {
        float raw = Objects.requireNonNull(value, "value");
        if (!Float.isFinite(raw)) {
            throw new IllegalArgumentException("value must be finite: " + raw);
        }
        return raw;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
