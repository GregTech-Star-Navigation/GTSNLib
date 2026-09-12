package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.LayoutEngine;
import com.gtsn.lib.ui.layout.MeasureSpec;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;

/**
 * 滚动容器：单内容子控件、垂直滚动（滚轮 / 滚动条拖拽）、视口裁剪与偏移重排。
 *
 * <p>滚动通过重排内容子树实现（内容包围盒随滚动量整体上移），因此渲染裁剪与
 * 命中测试天然一致——滚出视口的子控件既不会被绘制也不会被命中。</p>
 *
 * <p>内容测量在视口高度上不受 AT_MOST 钳制（按自然高度测量），保证长内容可完整滚动。</p>
 */
public final class ScrollPanelWidget extends AbstractWidget {

    private int scrollbarWidth = 6;
    private int scrollStep = 16;
    private int minThumbHeight = 10;
    private int background = 0xFF141414;
    private int borderColor = 0xFF3C4654;
    private int trackColor = 0xFF1E1E1E;
    private int thumbColor = 0xFF5A6470;
    private int activeThumbColor = 0xFF8FB0C8;

    private int scrollY;
    private int contentHeight;
    private boolean draggingThumb;
    private double dragStartY;
    private int dragStartScroll;

    public ScrollPanelWidget() {
    }

    public ScrollPanelWidget scrollbarWidth(int pixels) {
        if (pixels < 0) {
            throw new IllegalArgumentException("scrollbar width must be non-negative: " + pixels);
        }
        this.scrollbarWidth = pixels;
        return this;
    }

    public int scrollbarWidth() {
        return scrollbarWidth;
    }

    public ScrollPanelWidget scrollStep(int pixels) {
        if (pixels < 1) {
            throw new IllegalArgumentException("scroll step must be >= 1: " + pixels);
        }
        this.scrollStep = pixels;
        return this;
    }

    public int scrollStep() {
        return scrollStep;
    }

    public ScrollPanelWidget minThumbHeight(int pixels) {
        if (pixels < 1) {
            throw new IllegalArgumentException("min thumb height must be >= 1: " + pixels);
        }
        this.minThumbHeight = pixels;
        return this;
    }

    public ScrollPanelWidget colors(int background, int border, int track, int thumb, int activeThumb) {
        this.background = background;
        this.borderColor = border;
        this.trackColor = track;
        this.thumbColor = thumb;
        this.activeThumbColor = activeThumb;
        return this;
    }

    public ScrollPanelWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public ScrollPanelWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public ScrollPanelWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public ScrollPanelWidget padding(Insets padding) {
        node().params().padding(padding);
        return this;
    }

    public ScrollPanelWidget weight(float weight) {
        node().params().weight(weight);
        return this;
    }

    public int scrollY() {
        return scrollY;
    }

    /** 内容自然高度（视口高度无上限测量；布局后有效）。 */
    public int contentHeight() {
        return contentHeight;
    }

    /** 最大滚动量 = max(0, 内容高 - 视口高)。 */
    public int maxScroll() {
        return Math.max(0, contentHeight - viewportBounds().height());
    }

    public boolean isDraggingThumb() {
        return draggingThumb;
    }

    /** 滚动到指定偏移（钳制到 [0, maxScroll]）；返回滚动量是否变化。 */
    public boolean scrollTo(int target) {
        int clamped = Math.max(0, Math.min(target, maxScroll()));
        if (clamped == scrollY) {
            return false;
        }
        scrollY = clamped;
        applyScroll();
        return true;
    }

    /** 增量滚动（正值向下）；返回滚动量是否变化。 */
    public boolean scrollBy(int delta) {
        return scrollTo(scrollY + delta);
    }

    /** 内容视口（扣除滚动条槽位）。 */
    public Rect viewportBounds() {
        Rect area = bounds().inset(node().params().padding());
        return Rect.of(area.x(), area.y(), Math.max(0, area.width() - scrollbarWidth), area.height());
    }

    /** 滚动条轨道（视口右侧槽位）。 */
    public Rect trackBounds() {
        Rect area = bounds().inset(node().params().padding());
        int width = Math.min(scrollbarWidth, area.width());
        return Rect.of(area.right() - width, area.y(), width, area.height());
    }

    /** 滚动条滑块（无溢出时高度为 0）。 */
    public Rect thumbBounds() {
        Rect track = trackBounds();
        int max = maxScroll();
        if (max <= 0 || track.isEmpty()) {
            return Rect.of(track.x(), track.y(), track.width(), 0);
        }
        int viewportHeight = viewportBounds().height();
        int thumbHeight = (int) ((long) track.height() * viewportHeight / Math.max(1, contentHeight));
        thumbHeight = Math.max(minThumbHeight, Math.min(thumbHeight, track.height()));
        int travel = track.height() - thumbHeight;
        int offset = (int) ((long) travel * scrollY / max);
        return Rect.of(track.x(), track.y() + offset, track.width(), thumbHeight);
    }

    @Override
    public boolean clipsChildren() {
        return true;
    }

    @Override
    public void onLayout() {
        applyScroll();
    }

    /** 按自然高度测量内容并摆放到（视口 - 滚动量）处，随后钳制滚动量。 */
    private void applyScroll() {
        if (children().isEmpty()) {
            return;
        }
        Rect viewport = viewportBounds();
        if (viewport.width() <= 0 || viewport.height() <= 0) {
            return;
        }
        Widget content = children().get(0);
        Size measured = LayoutEngine.measure(content.node(),
                MeasureSpec.exactly(viewport.width()), MeasureSpec.unspecified());
        contentHeight = measured.height();
        scrollY = Math.max(0, Math.min(scrollY, maxScroll()));
        LayoutEngine.arrange(content.node(),
                Rect.of(viewport.x(), viewport.y() - scrollY, viewport.width(), contentHeight));
    }

    @Override
    public void render(RenderContext context) {
        onRender(context);
        Rect viewport = viewportBounds();
        if (!viewport.isEmpty()) {
            context.pushClip(viewport.x(), viewport.y(), viewport.width(), viewport.height());
        }
        for (Widget child : children()) {
            child.render(context);
        }
        if (!viewport.isEmpty()) {
            context.popClip();
        }
        drawScrollbar(context);
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        if (box.isEmpty()) {
            return;
        }
        if ((background >>> 24) != 0) {
            context.fill(box.x(), box.y(), box.width(), box.height(), background);
        }
        if ((borderColor >>> 24) != 0) {
            context.fill(box.x(), box.y(), box.width(), 1, borderColor);
            context.fill(box.x(), box.bottom() - 1, box.width(), 1, borderColor);
            context.fill(box.x(), box.y() + 1, 1, box.height() - 2, borderColor);
            context.fill(box.right() - 1, box.y() + 1, 1, box.height() - 2, borderColor);
        }
    }

    private void drawScrollbar(RenderContext context) {
        if (maxScroll() <= 0) {
            return;
        }
        Rect track = trackBounds();
        if (track.isEmpty()) {
            return;
        }
        context.fill(track.x(), track.y(), track.width(), track.height(), trackColor);
        Rect thumb = thumbBounds();
        if (thumb.height() > 0) {
            context.fill(thumb.x(), thumb.y(), thumb.width(), thumb.height(),
                    draggingThumb ? activeThumbColor : thumbColor);
        }
    }

    @Override
    public boolean onInput(InputEvent event) {
        if (event instanceof InputEvent.MouseScrolled scrolled) {
            if (maxScroll() <= 0 || !viewportBounds().contains(scrolled.x(), scrolled.y())) {
                return false;
            }
            return scrollBy((int) Math.round(-scrolled.scrollY() * scrollStep));
        }
        if (event instanceof InputEvent.MousePressed press
                && press.button() == 0 && thumbBounds().contains(press.x(), press.y())) {
            draggingThumb = true;
            dragStartY = press.y();
            dragStartScroll = scrollY;
            return true;
        }
        if (event instanceof InputEvent.MouseDragged drag && draggingThumb) {
            Rect track = trackBounds();
            int travel = Math.max(1, track.height() - thumbBounds().height());
            int delta = (int) Math.round((drag.y() - dragStartY) * maxScroll() / (double) travel);
            scrollTo(dragStartScroll + delta);
            return true;
        }
        if (event instanceof InputEvent.MouseReleased release && draggingThumb) {
            draggingThumb = false;
            return true;
        }
        return false;
    }
}
