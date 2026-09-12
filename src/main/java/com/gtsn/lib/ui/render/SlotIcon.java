package com.gtsn.lib.ui.render;

import com.gtsn.lib.ui.layout.Rect;

/**
 * 槽位图标绘制器：由调用方提供槽内内容的绘制方式（如物品堆叠），
 * 使 {@code ItemSlotWidget} 的槽底/状态逻辑与具体内容渲染解耦、可无 MC 测试。
 *
 * <p>实现类可位于客户端（真实物品渲染）或任意位置（程序化占位图形）。</p>
 */
@FunctionalInterface
public interface SlotIcon {

    /** 在给定图标区内绘制内容（调用方已保证不超出槽位）。 */
    void paint(RenderContext context, Rect bounds);
}
