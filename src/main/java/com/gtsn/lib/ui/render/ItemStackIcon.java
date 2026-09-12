package com.gtsn.lib.ui.render;

import com.gtsn.lib.ui.layout.Rect;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

/**
 * {@link SlotIcon} 的 Minecraft 实现：把物品堆叠（含数量/耐久装饰）渲染进槽位。
 *
 * <p>客户端专用类：依赖 {@link GuiGraphicsRenderContext}（GuiGraphics），专职服务端不得加载
 * （类加载纪律，见 ADR-0003/0004）。仅在客户端后端上下文中生效，其它实现被安全忽略。</p>
 */
public final class ItemStackIcon implements SlotIcon {

    private final ItemStack stack;

    private ItemStackIcon(ItemStack stack) {
        this.stack = Objects.requireNonNull(stack, "stack");
    }

    public static ItemStackIcon of(ItemStack stack) {
        return new ItemStackIcon(stack);
    }

    public ItemStack stack() {
        return stack;
    }

    @Override
    public void paint(RenderContext context, Rect bounds) {
        if (context instanceof GuiGraphicsRenderContext gui) {
            gui.graphics().renderItem(stack, bounds.x(), bounds.y());
            gui.graphics().renderItemDecorations(gui.font(), stack, bounds.x(), bounds.y());
        }
    }
}
