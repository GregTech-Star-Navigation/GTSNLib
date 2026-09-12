package com.gtsn.lib.ui.layout;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 布局引擎行为：盒模型（padding/margin）、固定/包裹/填充尺寸、权重分配、
 * 主轴/交叉轴对齐、嵌套栈与绝对锚定。
 *
 * <p>期望值均为手工推导的独立结果，不依赖实现内部结构。</p>
 */
class StackLayoutTest {

    private static LayoutNode fixed(int width, int height) {
        return new LayoutNode(LayoutParams.fixedSize(width, height));
    }

    @Test
    void layoutGivesRootTheFullAreaAndPlacesFixedChildAtContentOrigin() {
        LayoutNode root = new LayoutNode(LayoutParams.create());
        LayoutNode child = fixed(50, 20);
        root.addChild(child);

        LayoutEngine.layout(root, Rect.of(0, 0, 200, 100));

        assertEquals(Rect.of(0, 0, 200, 100), root.bounds());
        assertEquals(Rect.of(0, 0, 50, 20), child.bounds());
    }

    @Test
    void verticalStackAppliesPaddingMarginAndGap() {
        LayoutNode root = new LayoutNode(LayoutParams.create().padding(Insets.all(10)).gap(4));
        LayoutNode a = fixed(50, 20);
        LayoutNode b = new LayoutNode(LayoutParams.fixedSize(30, 10).margin(Insets.all(2)));
        root.addChild(a).addChild(b);

        assertEquals(Size.of(70, 58), LayoutEngine.measure(root, MeasureSpec.unspecified(), MeasureSpec.unspecified()));
        LayoutEngine.layout(root, Rect.of(0, 0, 70, 58));

        assertEquals(Rect.of(10, 10, 50, 20), a.bounds());
        assertEquals(Rect.of(12, 36, 30, 10), b.bounds(), "margin 负责偏移，前序节点下方叠加 gap");
        assertEquals(Rect.of(10, 10, 50, 38), root.contentBounds());
    }

    @Test
    void horizontalStackAlignsCrossAxisStartOrCenter() {
        LayoutNode start = new LayoutNode(LayoutParams.create().direction(Direction.HORIZONTAL).gap(6));
        LayoutNode a = fixed(20, 10);
        LayoutNode b = fixed(20, 30);
        start.addChild(a).addChild(b);
        assertEquals(Size.of(46, 30), LayoutEngine.measure(start, MeasureSpec.unspecified(), MeasureSpec.unspecified()));
        LayoutEngine.layout(start, Rect.of(0, 0, 46, 30));
        assertEquals(Rect.of(0, 0, 20, 10), a.bounds(), "默认交叉轴起点对齐");
        assertEquals(Rect.of(26, 0, 20, 30), b.bounds());

        LayoutNode center = new LayoutNode(LayoutParams.create().direction(Direction.HORIZONTAL).gap(6)
                .crossAxisAlign(CrossAxisAlign.CENTER));
        LayoutNode c = fixed(20, 10);
        LayoutNode d = fixed(20, 30);
        center.addChild(c).addChild(d);
        LayoutEngine.layout(center, Rect.of(0, 0, 46, 30));
        assertEquals(Rect.of(0, 10, 20, 10), c.bounds());
        assertEquals(Rect.of(26, 0, 20, 30), d.bounds());
    }

    @Test
    void stackAlignsCrossAxisToEnd() {
        LayoutNode root = new LayoutNode(LayoutParams.create().direction(Direction.HORIZONTAL)
                .crossAxisAlign(CrossAxisAlign.END));
        LayoutNode a = fixed(20, 10);
        LayoutNode b = fixed(20, 30);
        root.addChild(a).addChild(b);

        LayoutEngine.layout(root, Rect.of(0, 0, 46, 30));

        assertEquals(Rect.of(0, 20, 20, 10), a.bounds());
        assertEquals(Rect.of(20, 0, 20, 30), b.bounds(), "无 gap 时相邻子节点直接相接");
    }

    @Test
    void stretchCrossAxisFillsContentExtent() {
        LayoutNode root = new LayoutNode(LayoutParams.create().direction(Direction.HORIZONTAL)
                .crossAxisAlign(CrossAxisAlign.STRETCH));
        LayoutNode a = fixed(20, 10);
        LayoutNode b = fixed(20, 30);
        root.addChild(a).addChild(b);

        LayoutEngine.layout(root, Rect.of(0, 0, 100, 40));

        assertEquals(Rect.of(0, 0, 20, 40), a.bounds());
        assertEquals(Rect.of(20, 0, 20, 40), b.bounds());
    }

    @Test
    void mainAxisAlignCenterAndEndShiftWholeStack() {
        LayoutNode center = new LayoutNode(LayoutParams.create().mainAxisAlign(MainAxisAlign.CENTER));
        LayoutNode ca = fixed(20, 10);
        LayoutNode cb = fixed(20, 10);
        center.addChild(ca).addChild(cb);
        LayoutEngine.layout(center, Rect.of(0, 0, 100, 100));
        assertEquals(Rect.of(0, 40, 20, 10), ca.bounds());
        assertEquals(Rect.of(0, 50, 20, 10), cb.bounds());

        LayoutNode end = new LayoutNode(LayoutParams.create().mainAxisAlign(MainAxisAlign.END));
        LayoutNode ea = fixed(20, 10);
        end.addChild(ea);
        LayoutEngine.layout(end, Rect.of(0, 0, 100, 100));
        assertEquals(Rect.of(0, 90, 20, 10), ea.bounds());
    }

    @Test
    void spaceBetweenDistributesFreeSpaceBetweenChildren() {
        LayoutNode root = new LayoutNode(LayoutParams.create().mainAxisAlign(MainAxisAlign.SPACE_BETWEEN));
        LayoutNode a = fixed(20, 10);
        LayoutNode b = fixed(20, 10);
        LayoutNode c = fixed(20, 10);
        root.addChild(a).addChild(b).addChild(c);

        LayoutEngine.layout(root, Rect.of(0, 0, 100, 100));

        assertEquals(Rect.of(0, 0, 20, 10), a.bounds());
        assertEquals(Rect.of(0, 45, 20, 10), b.bounds());
        assertEquals(Rect.of(0, 90, 20, 10), c.bounds());
    }

    @Test
    void weightedChildReceivesRemainingMainAxisSpace() {
        LayoutNode root = new LayoutNode(LayoutParams.create());
        LayoutNode header = fixed(10, 20);
        LayoutNode body = new LayoutNode(LayoutParams.create().width(Sizing.fixed(10)).weight(1));
        root.addChild(header).addChild(body);

        LayoutEngine.layout(root, Rect.of(0, 0, 100, 100));

        assertEquals(Rect.of(0, 0, 10, 20), header.bounds());
        assertEquals(Rect.of(0, 20, 10, 80), body.bounds());
    }

    @Test
    void weightsSplitProportionallyAndRemainderGoesToEarlierChildren() {
        LayoutNode root = new LayoutNode(LayoutParams.create());
        LayoutNode one = new LayoutNode(LayoutParams.create().width(Sizing.fixed(10)).weight(1));
        LayoutNode two = new LayoutNode(LayoutParams.create().width(Sizing.fixed(10)).weight(2));
        root.addChild(one).addChild(two);

        LayoutEngine.layout(root, Rect.of(0, 0, 100, 100));

        assertEquals(Rect.of(0, 0, 10, 34), one.bounds(), "100 按 1:2 分配：33+66 余 1 给前者");
        assertEquals(Rect.of(0, 34, 10, 66), two.bounds());
    }

    @Test
    void mainAxisFillBehavesLikeWeightOne() {
        LayoutNode root = new LayoutNode(LayoutParams.create());
        LayoutNode header = fixed(10, 10);
        LayoutNode body = new LayoutNode(LayoutParams.create().width(Sizing.fixed(10)).height(Sizing.FILL));
        root.addChild(header).addChild(body);

        LayoutEngine.layout(root, Rect.of(0, 0, 100, 60));

        assertEquals(Rect.of(0, 10, 10, 50), body.bounds());
    }

    @Test
    void nestedStacksArrangeRecursively() {
        LayoutNode root = new LayoutNode(LayoutParams.create().padding(Insets.all(5)).gap(2));
        LayoutNode row = new LayoutNode(LayoutParams.create()
                .direction(Direction.HORIZONTAL).gap(5).width(Sizing.fixed(90)).height(Sizing.fixed(30)));
        LayoutNode cellA = fixed(10, 10);
        LayoutNode cellB = fixed(20, 10);
        row.addChild(cellA).addChild(cellB);
        LayoutNode below = fixed(40, 10);
        root.addChild(row).addChild(below);

        LayoutEngine.layout(root, Rect.of(0, 0, 100, 100));

        assertEquals(Rect.of(5, 5, 90, 30), row.bounds());
        assertEquals(Rect.of(5, 5, 10, 10), cellA.bounds());
        assertEquals(Rect.of(20, 5, 20, 10), cellB.bounds());
        assertEquals(Rect.of(5, 37, 40, 10), below.bounds());
    }

    @Test
    void wrapSizingUsesIntrinsicContentMeasurer() {
        List<MeasureSpec> capturedWidths = new ArrayList<>();
        LayoutNode leaf = new LayoutNode(LayoutParams.create().padding(Insets.all(2)));
        leaf.contentMeasurer((widthSpec, heightSpec) -> {
            capturedWidths.add(widthSpec);
            return Size.of(widthSpec.resolve(30), heightSpec.resolve(8));
        });

        Size measured = LayoutEngine.measure(leaf, MeasureSpec.atMost(20), MeasureSpec.unspecified());

        assertEquals(Size.of(20, 12), measured, "wrap = padding + 内容，再受 AT_MOST 截断");
        assertEquals(List.of(MeasureSpec.atMost(16)), capturedWidths, "固有测量收到扣除 padding 后的约束");
    }

    @Test
    void wrapSizingRespectsAtMostClamp() {
        LayoutNode root = new LayoutNode(LayoutParams.create());
        root.addChild(fixed(70, 10));

        Size measured = LayoutEngine.measure(root, MeasureSpec.atMost(40), MeasureSpec.unspecified());

        assertEquals(Size.of(40, 10), measured, "包裹内容超过上限时收敛到上限，子节点允许溢出");
    }

    @Test
    void stackedChildrenInsideFixedContainerRespectItsWidth() {
        LayoutNode root = new LayoutNode(LayoutParams.create().direction(Direction.HORIZONTAL));
        LayoutNode column = new LayoutNode(LayoutParams.create()
                .width(Sizing.fixed(100)).height(Sizing.wrap())
                .crossAxisAlign(CrossAxisAlign.STRETCH));
        LayoutNode panel = new LayoutNode(LayoutParams.create());
        LayoutNode fill = new LayoutNode(LayoutParams.create().fillWidth().height(Sizing.fixed(10)));
        panel.addChild(fill);
        column.addChild(panel);
        root.addChild(column);

        LayoutEngine.layout(root, Rect.of(0, 0, 300, 200));

        assertEquals(Size.of(100, 10), panel.measuredSize(), "固定容器的拉伸子节点按容器宽测量");
        assertEquals(Rect.of(0, 0, 100, 10), fill.bounds(), "嵌套填充子节点宽=固定容器宽，不受外层 300 约束");
    }

    @Test
    void absoluteChildIsAnchoredAndExcludedFromFlow() {
        LayoutNode root = new LayoutNode(LayoutParams.create().padding(Insets.all(5)));
        LayoutNode flow = fixed(20, 20);
        LayoutNode badge = new LayoutNode(LayoutParams.fixedSize(10, 10)
                .absolute(Anchor.BOTTOM_RIGHT, -2, -3));
        root.addChild(flow).addChild(badge);

        assertEquals(Size.of(30, 30), LayoutEngine.measure(root, MeasureSpec.unspecified(), MeasureSpec.unspecified()),
                "绝对定位子节点不参与父节点的包裹尺寸");
        LayoutEngine.layout(root, Rect.of(0, 0, 100, 80));

        assertEquals(Rect.of(5, 5, 20, 20), flow.bounds());
        assertEquals(Rect.of(83, 62, 10, 10), badge.bounds(), "锚定在内容区右下角并叠加偏移");
    }
}
