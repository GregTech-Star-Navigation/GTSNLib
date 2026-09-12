package com.gtsn.lib.ui.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 布局几何基础行为：矩形命中/收缩/相交、间距、测量约束解析、九宫格锚定。
 *
 * <p>全部为纯几何断言，不依赖 Minecraft。</p>
 */
class LayoutGeometryTest {

    @Test
    void rectExposesDerivedEdgesAndSize() {
        Rect rect = Rect.of(10, 20, 100, 50);

        assertEquals(110, rect.right());
        assertEquals(70, rect.bottom());
        assertEquals(Size.of(100, 50), rect.size());
        assertFalse(rect.isEmpty());
        assertTrue(Rect.ZERO.isEmpty());
    }

    @Test
    void rectContainsUsesHalfOpenBounds() {
        Rect rect = Rect.of(10, 20, 100, 50);

        assertTrue(rect.contains(10, 20), "左上角属于矩形");
        assertTrue(rect.contains(109.9, 69.9), "内部点属于矩形");
        assertFalse(rect.contains(110, 20), "右边界不属于矩形");
        assertFalse(rect.contains(10, 70), "下边界不属于矩形");
        assertFalse(rect.contains(9.99, 20), "左侧外部不属于矩形");
    }

    @Test
    void rectInsetShrinksEdgesAndClampsToZero() {
        assertEquals(Rect.of(15, 25, 90, 40), Rect.of(10, 20, 100, 50).inset(Insets.all(5)));
        assertEquals(Rect.of(5, 5, 0, 0), Rect.of(0, 0, 4, 4).inset(Insets.all(5)),
                "收缩量超出尺寸时退化到 0 而不是负数");
        assertEquals(Rect.of(13, 18, 100, 50), Rect.of(10, 20, 100, 50).offset(3, -2));
    }

    @Test
    void rectIntersectHandlesOverlapAndDisjoint() {
        Rect a = Rect.of(10, 10, 20, 20);

        assertEquals(Rect.of(15, 15, 15, 15), a.intersect(Rect.of(15, 15, 20, 20)));
        assertTrue(a.intersects(Rect.of(15, 15, 20, 20)));

        Rect disjoint = a.intersect(Rect.of(100, 100, 5, 5));
        assertEquals(0, disjoint.width());
        assertEquals(0, disjoint.height());
        assertFalse(a.intersects(Rect.of(100, 100, 5, 5)));
        assertFalse(a.intersects(Rect.of(30, 10, 5, 5)), "右边界相接不算相交");
    }

    @Test
    void insetsProvideCommonFactories() {
        Insets symmetric = Insets.symmetric(4, 8);

        assertEquals(4, symmetric.top());
        assertEquals(8, symmetric.right());
        assertEquals(4, symmetric.bottom());
        assertEquals(8, symmetric.left());
        assertEquals(8, symmetric.vertical());
        assertEquals(16, symmetric.horizontal());
        assertEquals(Insets.NONE, Insets.all(0));
    }

    @Test
    void measureSpecResolvesDesiredSizePerMode() {
        assertEquals(100, MeasureSpec.exactly(100).resolve(50), "EXACTLY 忽略期望值");
        assertEquals(100, MeasureSpec.atMost(100).resolve(150), "AT_MOST 上限截断");
        assertEquals(40, MeasureSpec.atMost(100).resolve(40), "AT_MOST 允许更小");
        assertEquals(77, MeasureSpec.unspecified().resolve(77), "UNSPECIFIED 接受期望值");
        assertEquals(0, MeasureSpec.unspecified().resolve(-5), "负期望值归一为 0");
    }

    @Test
    void measureSpecSubtractsReservedSpace() {
        MeasureSpec exactly = MeasureSpec.exactly(100).subtract(30);
        MeasureSpec atMost = MeasureSpec.atMost(100).subtract(30);

        assertEquals(MeasureSpec.Mode.EXACTLY, exactly.mode());
        assertEquals(70, exactly.size());
        assertEquals(MeasureSpec.Mode.AT_MOST, atMost.mode());
        assertEquals(70, atMost.size());
        assertEquals(0, MeasureSpec.exactly(10).subtract(30).size(), "扣除量超出约束时退化为 0");
        assertEquals(MeasureSpec.UNSPECIFIED, MeasureSpec.unspecified().subtract(30));
        assertTrue(MeasureSpec.exactly(1).isBounded());
        assertFalse(MeasureSpec.unspecified().isBounded());
    }

    @Test
    void geometryRejectsIllegalValues() {
        assertThrows(IllegalArgumentException.class, () -> new Size(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Rect(0, 0, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> new Insets(0, -1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> MeasureSpec.exactly(-1));
        assertThrows(IllegalArgumentException.class, () -> Sizing.fixed(-1));
        assertThrows(IllegalArgumentException.class, () -> LayoutParams.create().gap(-1));
        assertThrows(IllegalArgumentException.class, () -> LayoutParams.create().weight(-0.5f));
    }

    @Test
    void sizingFactoriesExposeExpectedVariants() {
        assertEquals(new Sizing.Fixed(12), Sizing.fixed(12));
        assertEquals(Sizing.WRAP, Sizing.wrap());
        assertEquals(Sizing.FILL, Sizing.fill());
    }

    @Test
    void directionMapsMainAndCrossAxes() {
        Size size = Size.of(30, 10);

        assertEquals(10, Direction.VERTICAL.main(size));
        assertEquals(30, Direction.VERTICAL.cross(size));
        assertEquals(30, Direction.HORIZONTAL.main(size));
        assertEquals(10, Direction.HORIZONTAL.cross(size));
    }

    @Test
    void anchorWithinPlacesAllNineAnchors() {
        Rect parent = Rect.of(0, 0, 200, 100);
        Size size = Size.of(40, 20);

        assertEquals(Rect.of(0, 0, 40, 20), LayoutEngine.anchorWithin(parent, size, Anchor.TOP_LEFT));
        assertEquals(Rect.of(80, 0, 40, 20), LayoutEngine.anchorWithin(parent, size, Anchor.TOP_CENTER));
        assertEquals(Rect.of(160, 0, 40, 20), LayoutEngine.anchorWithin(parent, size, Anchor.TOP_RIGHT));
        assertEquals(Rect.of(0, 40, 40, 20), LayoutEngine.anchorWithin(parent, size, Anchor.CENTER_LEFT));
        assertEquals(Rect.of(80, 40, 40, 20), LayoutEngine.anchorWithin(parent, size, Anchor.CENTER));
        assertEquals(Rect.of(160, 40, 40, 20), LayoutEngine.anchorWithin(parent, size, Anchor.CENTER_RIGHT));
        assertEquals(Rect.of(0, 80, 40, 20), LayoutEngine.anchorWithin(parent, size, Anchor.BOTTOM_LEFT));
        assertEquals(Rect.of(80, 80, 40, 20), LayoutEngine.anchorWithin(parent, size, Anchor.BOTTOM_CENTER));
        assertEquals(Rect.of(160, 80, 40, 20), LayoutEngine.anchorWithin(parent, size, Anchor.BOTTOM_RIGHT));
    }

    @Test
    void anchorWithinAppliesOffsetsAndParentOrigin() {
        Rect parent = Rect.of(10, 20, 200, 100);
        Size size = Size.of(40, 20);

        assertEquals(Rect.of(10, 20, 40, 20), LayoutEngine.anchorWithin(parent, size, Anchor.TOP_LEFT));
        assertEquals(Rect.of(175, 27, 40, 20),
                LayoutEngine.anchorWithin(parent, size, Anchor.TOP_RIGHT, 5, 7));
        assertEquals(Rect.of(15, 107, 40, 20),
                LayoutEngine.anchorWithin(parent, size, Anchor.BOTTOM_LEFT, 5, 7));
    }

    @Test
    void anchorWithinCentersWithFloorDivision() {
        Rect parent = Rect.of(0, 0, 11, 11);
        Size size = Size.of(4, 4);

        assertEquals(Rect.of(3, 3, 4, 4), LayoutEngine.anchorWithin(parent, size, Anchor.CENTER));
    }
}
