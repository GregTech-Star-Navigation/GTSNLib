package com.gtsn.lib.ui.screen;

import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.input.InputRouter;
import com.gtsn.lib.ui.layout.LayoutEngine;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.widget.Tooltip;
import com.gtsn.lib.ui.widget.Widget;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 控件泊点：持有一棵控件树、驱动布局（resize）、渲染与输入派发。
 *
 * <p>本类不依赖 Minecraft，客户端的 {@code GtsnScreen} 只是它的薄壳（转发生命周期回调与坐标）。</p>
 */
public final class WidgetHost {

    /** 工具提示内边距（像素）。 */
    public static final int TOOLTIP_PADDING = 4;

    private static final int TOOLTIP_BACKGROUND = 0xF0101418;
    private static final int TOOLTIP_BORDER = 0xFF8FB0C8;
    private static final int TOOLTIP_TEXT = 0xFFFFFFFF;

    private final Widget root;
    private final InputRouter router;
    private int width;
    private int height;

    public WidgetHost(Widget root) {
        this.root = Objects.requireNonNull(root, "root");
        this.router = new InputRouter(root);
    }

    public Widget root() {
        return root;
    }

    public InputRouter router() {
        return router;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    /** 按屏幕尺寸重新布局整棵树（根节点包围盒即屏幕）。 */
    public void resize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("host size must be non-negative: " + width + "x" + height);
        }
        this.width = width;
        this.height = height;
        LayoutEngine.layout(root.node(), Rect.of(0, 0, width, height));
    }

    /** 渲染整棵控件树。 */
    public void render(RenderContext context) {
        root.render(Objects.requireNonNull(context, "context"));
    }

    /** 派发输入事件；返回是否被消费。 */
    public boolean dispatch(InputEvent event) {
        return router.dispatch(Objects.requireNonNull(event, "event"));
    }

    /**
     * 当前悬停路径上最近的非空工具提示（从最深层向上解析；含工具提示的祖先可被子控件命中）。
     */
    public Optional<Tooltip> activeTooltip() {
        List<Widget> path = router.hoveredPath();
        for (int i = path.size() - 1; i >= 0; i--) {
            Tooltip tooltip = path.get(i).tooltip();
            if (tooltip != null && !tooltip.isEmpty()) {
                return Optional.of(tooltip);
            }
        }
        return Optional.empty();
    }

    /** 当前工具提示的绘制盒（鼠标跟随 + 屏幕边界钳制）；无提示时为空。 */
    public Optional<Rect> tooltipBounds(RenderContext context) {
        Tooltip tooltip = activeTooltip().orElse(null);
        if (tooltip == null) {
            return Optional.empty();
        }
        return Optional.of(layoutTooltip(tooltip, Objects.requireNonNull(context, "context")));
    }

    /**
     * 在控件树之上绘制当前工具提示（应在 {@link #render(RenderContext)} 之后调用，避免被控件覆盖）。
     */
    public void renderTooltips(RenderContext context) {
        Tooltip tooltip = activeTooltip().orElse(null);
        if (tooltip == null) {
            return;
        }
        Rect box = layoutTooltip(tooltip, context);
        context.fill(box.x(), box.y(), box.width(), box.height(), TOOLTIP_BACKGROUND);
        context.fill(box.x(), box.y(), box.width(), 1, TOOLTIP_BORDER);
        context.fill(box.x(), box.bottom() - 1, box.width(), 1, TOOLTIP_BORDER);
        context.fill(box.x(), box.y() + 1, 1, box.height() - 2, TOOLTIP_BORDER);
        context.fill(box.right() - 1, box.y() + 1, 1, box.height() - 2, TOOLTIP_BORDER);
        int lineY = box.y() + TOOLTIP_PADDING;
        for (String line : tooltip.lines()) {
            context.text(line, box.x() + TOOLTIP_PADDING, lineY, TOOLTIP_TEXT, false);
            lineY += context.textLineHeight();
        }
    }

    private Rect layoutTooltip(Tooltip tooltip, RenderContext context) {
        int contentWidth = 0;
        for (String line : tooltip.lines()) {
            contentWidth = Math.max(contentWidth, context.textWidth(line));
        }
        int boxWidth = contentWidth + 2 * TOOLTIP_PADDING;
        int boxHeight = tooltip.lines().size() * context.textLineHeight() + 2 * TOOLTIP_PADDING;
        int mouseX = (int) router.mouseX();
        int mouseY = (int) router.mouseY();
        int x = mouseX + 10;
        int y = mouseY - 4;
        if (x + boxWidth > context.width()) {
            x = mouseX - boxWidth - 8;
        }
        if (y + boxHeight > context.height()) {
            y = context.height() - boxHeight - 2;
        }
        x = Math.max(0, Math.min(x, Math.max(0, context.width() - boxWidth)));
        y = Math.max(0, Math.min(y, Math.max(0, context.height() - boxHeight)));
        return Rect.of(x, y, boxWidth, boxHeight);
    }
}
