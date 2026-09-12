package com.gtsn.lib.ui.sync;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 编解码器单测：int / 区间 int、float / 区间 float、bool、枚举与索引助手。
 *
 * <p>验证外部可观察行为：任意输入经 encode/decode 得到合法类型化值，钳制与兜底规则稳定。</p>
 */
class SyncCodecsTest {

    private enum Phase {
        IDLE, RUNNING, DONE
    }

    @Test
    void intsRoundTripAnyValue() {
        SyncCodec<Integer> codec = SyncCodecs.ints();
        assertEquals(12345, codec.encode(12345));
        assertEquals(-7, codec.decode(-7));
        assertEquals(Integer.MAX_VALUE, codec.decode(codec.encode(Integer.MAX_VALUE)));
    }

    @Test
    void intRangeClampsOnEncodeAndDecode() {
        SyncCodec<Integer> codec = SyncCodecs.intRange(0, 100);
        assertEquals(0, codec.encode(-5));
        assertEquals(100, codec.encode(150));
        assertEquals(42, codec.encode(42));
        assertEquals(0, codec.decode(-1));
        assertEquals(100, codec.decode(101));
        assertEquals(42, codec.decode(42));
    }

    @Test
    void intRangeRejectsEmptyOrInvertedRange() {
        assertThrows(IllegalArgumentException.class, () -> SyncCodecs.intRange(10, 10));
        assertThrows(IllegalArgumentException.class, () -> SyncCodecs.intRange(10, 5));
    }

    @Test
    void floatsRoundTripExactlyIncludingEdgeValues() {
        SyncCodec<Float> codec = SyncCodecs.floats();
        assertEquals(1.5f, codec.decode(codec.encode(1.5f)), 0f);
        assertEquals(-123.25f, codec.decode(codec.encode(-123.25f)), 0f);
        assertEquals(Float.MAX_VALUE, codec.decode(codec.encode(Float.MAX_VALUE)), 0f);
        assertEquals(Float.floatToRawIntBits(-0.0f), codec.encode(-0.0f));
    }

    @Test
    void floatsRejectNonFiniteOnEncode() {
        SyncCodec<Float> codec = SyncCodecs.floats();
        assertThrows(IllegalArgumentException.class, () -> codec.encode(Float.NaN));
        assertThrows(IllegalArgumentException.class, () -> codec.encode(Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> codec.encode(Float.NEGATIVE_INFINITY));
    }

    @Test
    void floatsFallBackToZeroOnNonFiniteDecode() {
        SyncCodec<Float> codec = SyncCodecs.floats();
        assertEquals(0f, codec.decode(Float.floatToRawIntBits(Float.NaN)));
        assertEquals(0f, codec.decode(Float.floatToRawIntBits(Float.POSITIVE_INFINITY)));
        assertEquals(0f, codec.decode(Float.floatToRawIntBits(Float.NEGATIVE_INFINITY)));
    }

    @Test
    void floatRangeClampsAndGuardsNonFinite() {
        SyncCodec<Float> codec = SyncCodecs.floatRange(0f, 5f);
        assertEquals(0f, Float.intBitsToFloat(codec.encode(-1f)), 0f);
        assertEquals(5f, Float.intBitsToFloat(codec.encode(9f)), 0f);
        assertEquals(2.5f, codec.decode(codec.encode(2.5f)), 0f);
        assertEquals(5f, codec.decode(Float.floatToRawIntBits(7f)));
        assertEquals(0f, codec.decode(Float.floatToRawIntBits(Float.NaN)));
        assertThrows(IllegalArgumentException.class, () -> codec.encode(Float.NaN));
    }

    @Test
    void boolsEncodeToZeroOrOneAndDecodeAnyNonZeroAsTrue() {
        SyncCodec<Boolean> codec = SyncCodecs.bools();
        assertEquals(1, codec.encode(true));
        assertEquals(0, codec.encode(false));
        assertFalse(codec.decode(0));
        assertTrue(codec.decode(1));
        assertTrue(codec.decode(-5));
    }

    @Test
    void enumsUseOrdinalAndFallBackToFirstConstant() {
        SyncCodec<Phase> codec = SyncCodecs.enums(Phase.class);
        assertEquals(1, codec.encode(Phase.RUNNING));
        assertSame(Phase.RUNNING, codec.decode(1));
        assertSame(Phase.IDLE, codec.decode(-1));
        assertSame(Phase.IDLE, codec.decode(Phase.values().length));
    }

    @Test
    void enumsAcceptExplicitFallback() {
        SyncCodec<Phase> codec = SyncCodecs.enums(Phase.class, Phase.DONE);
        assertSame(Phase.DONE, codec.decode(99));
        assertSame(Phase.IDLE, codec.decode(0));
    }

    @Test
    void enumsRejectNullTypeOrFallback() {
        assertThrows(NullPointerException.class, () -> SyncCodecs.enums(null));
        assertThrows(NullPointerException.class, () -> SyncCodecs.enums(Phase.class, null));
    }

    @Test
    void indexClampsToSize() {
        SyncCodec<Integer> codec = SyncCodecs.index(5);
        assertEquals(0, codec.encode(-1));
        assertEquals(4, codec.encode(5));
        assertEquals(3, codec.encode(3));
        assertEquals(4, codec.decode(10));
        assertEquals(0, codec.decode(-10));
    }

    @Test
    void indexRejectsNonPositiveSize() {
        assertThrows(IllegalArgumentException.class, () -> SyncCodecs.index(0));
        assertThrows(IllegalArgumentException.class, () -> SyncCodecs.index(-1));
    }
}
