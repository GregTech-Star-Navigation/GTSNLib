package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.input.Keys;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.render.SlotIcon;

import java.util.Objects;

/**
 * 物品槽控件：槽底（原版风格内凹边框）+ 可插拔图标 + 悬停/按下/选中/禁用状态。
 *
 * <p>内容通过 {@link SlotIcon} 绘制（客户端可传入 {@code ItemStackIcon} 渲染真实物品堆叠）；
 * 槽位本身不依赖 Minecraft，状态逻辑可无 MC 测试。</p>
 *
 * <p>交互模式：默认纯展示（不聚焦、不消费输入）；{@link #selectable(boolean)} 或
 * {@link #onClick(Runnable)} 使其可交互——点击（或聚焦时 Enter/Space）切换选择/触发回调。</p>
 */
public final class ItemSlotWidget extends AbstractWidget {

    private SlotIcon icon;
    private int width = 18;
    private int height = 18;

    private boolean enabled = true;
    private boolean selectable;
    private boolean selected;
    private boolean hovered;
    private boolean pressed;
    private boolean focused;

    private Runnable onClick;
    private BooleanConsumer onSelectionChanged;

    public ItemSlotWidget() {
        node().contentMeasurer((widthSpec, heightSpec) ->
                Size.of(widthSpec.resolve(width), heightSpec.resolve(height)));
    }

    /** 固定正方形槽位（默认 18x18）。 */
    public ItemSlotWidget slotSize(int size) {
        if (size < 4) {
            throw new IllegalArgumentException("slot size must be >= 4: " + size);
        }
        this.width = size;
        this.height = size;
        return this;
    }

    public ItemSlotWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public ItemSlotWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public ItemSlotWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public ItemSlotWidget weight(float weight) {
        node().params().weight(weight);
        return this;
    }

    public ItemSlotWidget icon(SlotIcon icon) {
        this.icon = icon;
        return this;
    }

    public SlotIcon icon() {
        return icon;
    }

    public boolean isEmpty() {
        return icon == null;
    }

    public ItemSlotWidget enabled(boolean enabled) {
        this.enabled = enabled;
        if (!enabled) {
            pressed = false;
        }
        return this;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public ItemSlotWidget selectable(boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    public boolean isSelectable() {
        return selectable;
    }

    public ItemSlotWidget selected(boolean selected) {
        this.selected = selected;
        return this;
    }

    public boolean isSelected() {
        return selected;
    }

    public ItemSlotWidget onClick(Runnable onClick) {
        this.onClick = Objects.requireNonNull(onClick, "onClick");
        return this;
    }

    public ItemSlotWidget onSelectionChanged(BooleanConsumer listener) {
        this.onSelectionChanged = Objects.requireNonNull(listener, "listener");
        return this;
    }

    public boolean isHovered() {
        return hovered;
    }

    public boolean isPressed() {
        return pressed;
    }

    public boolean isFocused() {
        return focused;
    }

    /** 图标绘制区：槽内缩 1px（原版槽位内衬）。 */
    public Rect iconBounds() {
        return bounds().inset(Insets.all(1));
    }

    private boolean interactive() {
        return enabled && (selectable || onClick != null);
    }

    @Override
    public boolean isFocusable() {
        return interactive();
    }

    @Override
    public void onFocusChanged(boolean focused) {
        this.focused = focused;
    }

    @Override
    public void onHoverChanged(boolean hovered) {
        this.hovered = hovered;
    }

    @Override
    public boolean onInput(InputEvent event) {
        if (!interactive()) {
            return false;
        }
        if (event instanceof InputEvent.MousePressed press
                && press.button() == 0 && bounds().contains(press.x(), press.y())) {
            pressed = true;
            return true;
        }
        if (event instanceof InputEvent.MouseReleased release && release.button() == 0 && pressed) {
            pressed = false;
            if (bounds().contains(release.x(), release.y())) {
                activate();
            }
            return true;
        }
        if (event instanceof InputEvent.KeyPressed key && focused
                && (key.keyCode() == Keys.ENTER || key.keyCode() == Keys.SPACE)) {
            activate();
            return true;
        }
        return false;
    }

    private void activate() {
        if (selectable) {
            selected = !selected;
            if (onSelectionChanged != null) {
                onSelectionChanged.accept(selected);
            }
        }
        if (onClick != null) {
            onClick.run();
        }
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        if (box.isEmpty()) {
            return;
        }
        drawSlotBackground(context, box);
        SlotIcon current = icon;
        if (current != null) {
            Rect iconArea = box.inset(Insets.all(1));
            if (!iconArea.isEmpty()) {
                current.paint(context, iconArea);
            }
        }
        if (enabled && hovered) {
            context.fill(box.x(), box.y(), box.width(), box.height(), 0x40FFFFFF);
        }
        if (enabled && pressed) {
            context.fill(box.x(), box.y(), box.width(), box.height(), 0x30000000);
        }
        if (!enabled) {
            context.fill(box.x(), box.y(), box.width(), box.height(), 0x80000000);
        }
        if (selected) {
            drawBorder(context, box, 1, 0xFFFFFFFF);
        } else if (focused) {
            drawBorder(context, box, 1, 0xFF7FB8FF);
        }
    }

    /** 原版背包槽风格：浅灰底 + 上左暗边 + 下右亮边。 */
    private static void drawSlotBackground(RenderContext context, Rect box) {
        context.fill(box.x(), box.y(), box.width(), box.height(), 0xFF8B8B8B);
        context.fill(box.x(), box.y(), box.width(), 1, 0xFF373737);
        context.fill(box.x(), box.y(), 1, box.height(), 0xFF373737);
        context.fill(box.x(), box.bottom() - 1, box.width(), 1, 0xFFFFFFFF);
        context.fill(box.right() - 1, box.y(), 1, box.height(), 0xFFFFFFFF);
    }

    private static void drawBorder(RenderContext context, Rect box, int thickness, int color) {
        int t = Math.min(thickness, Math.min(box.width(), box.height()));
        if (t <= 0) {
            return;
        }
        context.fill(box.x(), box.y(), box.width(), t, color);
        context.fill(box.x(), box.bottom() - t, box.width(), t, color);
        context.fill(box.x(), box.y() + t, t, box.height() - 2 * t, color);
        context.fill(box.right() - t, box.y() + t, t, box.height() - 2 * t, color);
    }
}
