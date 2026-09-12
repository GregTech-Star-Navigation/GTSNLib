package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Anchor;
import com.gtsn.lib.ui.layout.CrossAxisAlign;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.MainAxisAlign;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;

import java.util.Objects;

/**
 * 面板容器：背景 + 边框 + 可选标题头的分组容器，子控件按栈布局排列。
 *
 * <p>与 {@link Stack} 的区别在于自带容器绘制与标题头；标题占用头部高度，
 * 内容区自动下移（用户 padding 之上叠加头部高度）。</p>
 */
public final class PanelWidget extends AbstractWidget {

    private int background;
    private int borderColor;
    private int borderWidth = 1;
    private String title;
    private TextMetrics titleMetrics;
    private int titleColor = 0xFFF0F0F0;
    private int headerFill = 0xFF262C36;
    private int headerRule = 0xFF3C4654;
    private Insets userPadding = Insets.NONE;

    public PanelWidget() {
    }

    public PanelWidget background(int argb) {
        this.background = argb;
        return this;
    }

    public PanelWidget border(int argb, int thickness) {
        this.borderColor = argb;
        this.borderWidth = Math.max(0, thickness);
        return this;
    }

    public PanelWidget title(String title, TextMetrics metrics) {
        this.title = Objects.requireNonNull(title, "title");
        this.titleMetrics = Objects.requireNonNull(metrics, "metrics");
        applyPadding();
        return this;
    }

    public String title() {
        return title;
    }

    public PanelWidget titleColor(int argb) {
        this.titleColor = argb;
        return this;
    }

    public PanelWidget headerColors(int fill, int rule) {
        this.headerFill = fill;
        this.headerRule = rule;
        return this;
    }

    public PanelWidget padding(Insets padding) {
        this.userPadding = Objects.requireNonNull(padding, "padding");
        applyPadding();
        return this;
    }

    public PanelWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public PanelWidget gap(int gap) {
        node().params().gap(gap);
        return this;
    }

    public PanelWidget direction(com.gtsn.lib.ui.layout.Direction direction) {
        node().params().direction(direction);
        return this;
    }

    public PanelWidget align(MainAxisAlign main, CrossAxisAlign cross) {
        node().params().mainAxisAlign(main).crossAxisAlign(cross);
        return this;
    }

    public PanelWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public PanelWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public PanelWidget fill() {
        node().params().fill();
        return this;
    }

    public PanelWidget fillWidth() {
        node().params().fillWidth();
        return this;
    }

    public PanelWidget weight(float weight) {
        node().params().weight(weight);
        return this;
    }

    public PanelWidget absolute(Anchor anchor, int offsetX, int offsetY) {
        node().params().absolute(anchor, offsetX, offsetY);
        return this;
    }

    /** 标题头高度（无标题为 0）。 */
    public int headerHeight() {
        return title == null ? 0 : titleMetrics.lineHeight() + 6;
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        int width = box.width();
        int height = box.height();
        if ((background >>> 24) != 0 && width > 0 && height > 0) {
            context.fill(box.x(), box.y(), width, height, background);
        }
        if (borderWidth > 0 && (borderColor >>> 24) != 0 && width > 0 && height > 0) {
            int thickness = Math.min(borderWidth, Math.min(width, height));
            context.fill(box.x(), box.y(), width, thickness, borderColor);
            context.fill(box.x(), box.bottom() - thickness, width, thickness, borderColor);
            context.fill(box.x(), box.y() + thickness, thickness, height - 2 * thickness, borderColor);
            context.fill(box.right() - thickness, box.y() + thickness, thickness,
                    height - 2 * thickness, borderColor);
        }
        if (title != null && width > 0 && height > 0) {
            int header = Math.min(headerHeight(), height);
            int headerTop = box.y() + userPadding.top();
            context.fill(box.x(), headerTop, width, header, headerFill);
            if (header > 0) {
                context.fill(box.x(), headerTop + header - 1, width, 1, headerRule);
            }
            int textY = headerTop + Math.max(0, (header - context.textLineHeight()) / 2);
            context.text(title, box.x() + userPadding.left(), textY, titleColor, false);
        }
    }

    private void applyPadding() {
        int header = headerHeight();
        node().params().padding(
                new Insets(userPadding.top() + header, userPadding.right(), userPadding.bottom(), userPadding.left()));
    }
}
