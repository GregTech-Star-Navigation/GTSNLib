package com.gtsn.lib.ui.layout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 布局引擎：自底向上测量（measure） + 自顶向下摆放（arrange）。
 *
 * <p>盒模型：节点尺寸（不含 margin）由 双轴尺寸策略 + padding + 内容/子节点 决定；
 * 栈容器沿主轴排列在流子节点（gap/主轴对齐/权重/填充参与分配），交叉轴按对齐规则定位或拉伸。
 * 绝对定位子节点（{@link LayoutParams#absolute}）脱离流，按九宫格锚点相对父内容区放置。</p>
 *
 * <p>纯几何实现，不依赖 Minecraft；测量结果写入 {@link LayoutNode#measuredSize()}，
 * 摆放结果写入 {@link LayoutNode#bounds()}。</p>
 */
public final class LayoutEngine {

    private LayoutEngine() {
    }

    /** 在给定约束下测量节点及其子树，返回节点外部尺寸（含 padding，不含 margin）。 */
    public static Size measure(LayoutNode node, MeasureSpec widthSpec, MeasureSpec heightSpec) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(widthSpec, "widthSpec");
        Objects.requireNonNull(heightSpec, "heightSpec");

        LayoutParams params = node.params();
        Insets padding = params.padding();
        MeasureSpec innerWidthSpec = widthSpec.subtract(padding.horizontal());
        MeasureSpec innerHeightSpec = heightSpec.subtract(padding.vertical());
        Direction direction = params.direction();
        List<LayoutNode> flow = flowChildren(node);

        // 第一轮：按自然约束测量全部在流子节点。
        for (LayoutNode child : flow) {
            measureChild(child, direction, innerWidthSpec, innerHeightSpec, params.crossAxisAlign(), null);
        }

        // 主轴有界时按权重/填充重新分配剩余空间；主轴无界则保持自然尺寸。
        MeasureSpec innerMainSpec = mainSpec(innerWidthSpec, innerHeightSpec, direction);
        if (!flow.isEmpty() && innerMainSpec.isBounded()) {
            distributeMainAxis(flow, direction, params, innerWidthSpec, innerHeightSpec, innerMainSpec.size());
        }

        // 汇总内容区尺寸：子节点占位（含 margin）与固有内容取各轴较大者。
        int childrenMain = 0;
        int childrenCross = 0;
        for (LayoutNode child : flow) {
            Size size = child.measuredSize();
            Insets margin = child.params().margin();
            childrenMain += direction.main(size) + direction.main(margin);
            childrenCross = Math.max(childrenCross, direction.cross(size) + direction.cross(margin));
        }
        if (flow.size() > 1) {
            childrenMain += params.gap() * (flow.size() - 1);
        }

        Size childrenExtent = direction == Direction.VERTICAL
                ? Size.of(childrenCross, childrenMain)
                : Size.of(childrenMain, childrenCross);
        int contentWidth = childrenExtent.width();
        int contentHeight = childrenExtent.height();
        ContentMeasurer measurer = node.contentMeasurer();
        if (measurer != null) {
            Size intrinsic = measurer.measure(innerWidthSpec, innerHeightSpec);
            contentWidth = Math.max(contentWidth, intrinsic.width());
            contentHeight = Math.max(contentHeight, intrinsic.height());
        }

        Size measured = Size.of(
                resolveAxis(params.width(), widthSpec, padding.horizontal() + contentWidth),
                resolveAxis(params.height(), heightSpec, padding.vertical() + contentHeight));
        node.setMeasuredSize(measured);
        return measured;
    }

    /** 把节点摆放到给定区域（绝对坐标），并递归摆放子树。 */
    public static void arrange(LayoutNode node, Rect area) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(area, "area");

        node.setBounds(area);
        LayoutParams params = node.params();
        Direction direction = params.direction();
        Rect content = area.inset(params.padding());
        List<LayoutNode> flow = flowChildren(node);

        int totalMain = 0;
        for (LayoutNode child : flow) {
            totalMain += direction.main(child.measuredSize()) + direction.main(child.params().margin());
        }
        if (flow.size() > 1) {
            totalMain += params.gap() * (flow.size() - 1);
        }

        int free = Math.max(0, direction.main(content.size()) - totalMain);
        int cursor = direction == Direction.VERTICAL ? content.y() : content.x();
        int extraGap = 0;
        switch (params.mainAxisAlign()) {
            case START -> {
            }
            case CENTER -> cursor += free / 2;
            case END -> cursor += free;
            case SPACE_BETWEEN -> {
                if (flow.size() > 1) {
                    extraGap = free / (flow.size() - 1);
                }
            }
        }

        int contentCross = direction.cross(content.size());
        int crossStart = direction == Direction.VERTICAL ? content.x() : content.y();

        for (LayoutNode child : flow) {
            Insets margin = child.params().margin();
            Size size = child.measuredSize();
            int childMain = direction.main(size);
            int childCross = direction.cross(size);
            int mainPos = cursor + lead(margin, direction);
            int crossPos;
            int placedCross = childCross;
            switch (params.crossAxisAlign()) {
                case START -> crossPos = crossStart + crossLead(margin, direction);
                case CENTER -> crossPos = crossStart
                        + (contentCross - childCross - crossSum(margin, direction)) / 2
                        + crossLead(margin, direction);
                case END -> crossPos = crossStart + contentCross - crossTrail(margin, direction) - childCross;
                case STRETCH -> {
                    placedCross = Math.max(0, contentCross - crossSum(margin, direction));
                    crossPos = crossStart + crossLead(margin, direction);
                }
                default -> throw new IllegalStateException("unreachable");
            }
            Rect childRect = direction == Direction.VERTICAL
                    ? Rect.of(crossPos, mainPos, placedCross, childMain)
                    : Rect.of(mainPos, crossPos, childMain, placedCross);
            arrange(child, childRect);
            cursor = mainPos + childMain + trail(margin, direction) + params.gap() + extraGap;
        }

        for (LayoutNode child : absoluteChildren(node)) {
            if (!child.isMeasured()) {
                measure(child, MeasureSpec.atMost(content.width()), MeasureSpec.atMost(content.height()));
            }
            Rect childRect = anchorWithin(content, child.measuredSize(), child.params().anchor(),
                    child.params().anchorOffsetX(), child.params().anchorOffsetY());
            arrange(child, childRect);
        }
    }

    /** 常用入口：以区域尺寸精确测量后摆放（根节点包围盒即区域）。 */
    public static void layout(LayoutNode node, Rect area) {
        Objects.requireNonNull(node, "node");
        Objects.requireNonNull(area, "area");
        measure(node, MeasureSpec.exactly(area.width()), MeasureSpec.exactly(area.height()));
        arrange(node, area);
    }

    /** 九宫格锚定：把尺寸为 size 的矩形锚定到父矩形对应方位（无偏移）。 */
    public static Rect anchorWithin(Rect parent, Size size, Anchor anchor) {
        return anchorWithin(parent, size, anchor, 0, 0);
    }

    /** 九宫格锚定并附加像素偏移（偏移对任意锚点同向生效）。 */
    public static Rect anchorWithin(Rect parent, Size size, Anchor anchor, int offsetX, int offsetY) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(size, "size");
        Objects.requireNonNull(anchor, "anchor");

        int x = switch (anchor) {
            case TOP_LEFT, CENTER_LEFT, BOTTOM_LEFT -> parent.x() + offsetX;
            case TOP_CENTER, CENTER, BOTTOM_CENTER -> parent.x() + (parent.width() - size.width()) / 2 + offsetX;
            case TOP_RIGHT, CENTER_RIGHT, BOTTOM_RIGHT -> parent.x() + parent.width() - size.width() + offsetX;
        };
        int y = switch (anchor) {
            case TOP_LEFT, TOP_CENTER, TOP_RIGHT -> parent.y() + offsetY;
            case CENTER_LEFT, CENTER, CENTER_RIGHT -> parent.y() + (parent.height() - size.height()) / 2 + offsetY;
            case BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT -> parent.y() + parent.height() - size.height() + offsetY;
        };
        return new Rect(x, y, size.width(), size.height());
    }

    private static void measureChild(LayoutNode child, Direction direction,
                                     MeasureSpec innerWidthSpec, MeasureSpec innerHeightSpec,
                                     CrossAxisAlign crossAlign, MeasureSpec mainOverride) {
        Sizing crossSizing = direction == Direction.VERTICAL ? child.params().width() : child.params().height();
        boolean fillCross = crossAlign == CrossAxisAlign.STRETCH || crossSizing instanceof Sizing.Fill;
        if (direction == Direction.VERTICAL) {
            MeasureSpec widthSpec = fillCross && innerWidthSpec.isBounded()
                    ? MeasureSpec.exactly(innerWidthSpec.size())
                    : relax(innerWidthSpec);
            MeasureSpec heightSpec = mainOverride != null ? mainOverride : relax(innerHeightSpec);
            measure(child, widthSpec, heightSpec);
        } else {
            MeasureSpec heightSpec = fillCross && innerHeightSpec.isBounded()
                    ? MeasureSpec.exactly(innerHeightSpec.size())
                    : relax(innerHeightSpec);
            MeasureSpec widthSpec = mainOverride != null ? mainOverride : relax(innerWidthSpec);
            measure(child, widthSpec, heightSpec);
        }
    }

    private static void distributeMainAxis(List<LayoutNode> flow, Direction direction, LayoutParams parentParams,
                                           MeasureSpec innerWidthSpec, MeasureSpec innerHeightSpec, int budget) {
        double totalWeight = 0;
        int used = 0;
        for (LayoutNode child : flow) {
            Insets margin = child.params().margin();
            used += direction.main(margin);
            double weight = effectiveWeight(child, direction);
            if (weight > 0) {
                totalWeight += weight;
            } else {
                used += direction.main(child.measuredSize());
            }
        }
        if (flow.size() > 1) {
            used += parentParams.gap() * (flow.size() - 1);
        }

        int leftover = budget - used;
        if (leftover <= 0 || totalWeight <= 0) {
            return;
        }

        List<LayoutNode> weighted = new ArrayList<>();
        List<Integer> allocations = new ArrayList<>();
        int assigned = 0;
        for (LayoutNode child : flow) {
            double weight = effectiveWeight(child, direction);
            if (weight <= 0) {
                continue;
            }
            int share = (int) Math.floor(leftover * weight / totalWeight);
            weighted.add(child);
            allocations.add(share);
            assigned += share;
        }
        int remainder = leftover - assigned;
        for (int i = 0; i < weighted.size() && remainder > 0; i++) {
            allocations.set(i, allocations.get(i) + 1);
            remainder--;
        }
        for (int i = 0; i < weighted.size(); i++) {
            measureChild(weighted.get(i), direction, innerWidthSpec, innerHeightSpec,
                    parentParams.crossAxisAlign(), MeasureSpec.exactly(allocations.get(i)));
        }
    }

    private static double effectiveWeight(LayoutNode child, Direction direction) {
        LayoutParams params = child.params();
        if (params.weight() > 0) {
            return params.weight();
        }
        Sizing main = direction == Direction.VERTICAL ? params.height() : params.width();
        return main instanceof Sizing.Fill ? 1.0 : 0.0;
    }

    /**
     * 单轴尺寸解析：EXACTLY 约束优先（拉伸/权重强制），其次显式固定/填充策略，
     * 包裹策略在 AT_MOST 下截断、UNSPECIFIED 下取内容尺寸。固定尺寸允许超出 AT_MOST（溢出）。
     */
    private static int resolveAxis(Sizing sizing, MeasureSpec spec, int wrap) {
        if (spec.mode() == MeasureSpec.Mode.EXACTLY) {
            return spec.size();
        }
        if (sizing instanceof Sizing.Fixed fixed) {
            return fixed.pixels();
        }
        if (sizing instanceof Sizing.Fill) {
            return spec.isBounded() ? spec.size() : Math.max(0, wrap);
        }
        int desired = Math.max(0, wrap);
        return spec.mode() == MeasureSpec.Mode.AT_MOST ? Math.min(spec.size(), desired) : desired;
    }

    private static MeasureSpec relax(MeasureSpec spec) {
        return spec.mode() == MeasureSpec.Mode.EXACTLY ? MeasureSpec.atMost(spec.size()) : spec;
    }

    private static MeasureSpec mainSpec(MeasureSpec widthSpec, MeasureSpec heightSpec, Direction direction) {
        return direction == Direction.VERTICAL ? heightSpec : widthSpec;
    }

    private static List<LayoutNode> flowChildren(LayoutNode node) {
        List<LayoutNode> result = new ArrayList<>();
        for (LayoutNode child : node.children()) {
            if (!child.isAbsolute()) {
                result.add(child);
            }
        }
        return result;
    }

    private static List<LayoutNode> absoluteChildren(LayoutNode node) {
        List<LayoutNode> result = new ArrayList<>();
        for (LayoutNode child : node.children()) {
            if (child.isAbsolute()) {
                result.add(child);
            }
        }
        return result;
    }

    private static int lead(Insets margin, Direction direction) {
        return direction == Direction.VERTICAL ? margin.top() : margin.left();
    }

    private static int trail(Insets margin, Direction direction) {
        return direction == Direction.VERTICAL ? margin.bottom() : margin.right();
    }

    private static int crossLead(Insets margin, Direction direction) {
        return direction == Direction.VERTICAL ? margin.left() : margin.top();
    }

    private static int crossTrail(Insets margin, Direction direction) {
        return direction == Direction.VERTICAL ? margin.right() : margin.bottom();
    }

    private static int crossSum(Insets margin, Direction direction) {
        return crossLead(margin, direction) + crossTrail(margin, direction);
    }
}
