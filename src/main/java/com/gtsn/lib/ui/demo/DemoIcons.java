package com.gtsn.lib.ui.demo;

import com.gtsn.lib.ui.render.SlotIcon;

/**
 * 开发测试界面的物品图标来源：客户端传入真实物品堆叠渲染，GameTest/单测使用程序化占位图形。
 *
 * <p>该接口为客户端接缝：{@code DemoContent} 本身保持 MC-free，客户端实现可引用
 * {@code ItemStackIcon} 等客户端专用类。</p>
 */
public interface DemoIcons {

    SlotIcon primary();

    SlotIcon secondary();

    /** 程序化占位图标（无 MC 环境使用）。 */
    static DemoIcons placeholders() {
        SlotIcon primary = (context, bounds) -> {
            context.fill(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF16222C);
            context.fill(bounds.x() + 3, bounds.y() + 3, bounds.width() - 6, bounds.height() - 6, 0xFF5FD3F3);
        };
        SlotIcon secondary = (context, bounds) -> {
            context.fill(bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0xFF2C1616);
            context.fill(bounds.x() + 3, bounds.y() + 3, bounds.width() - 6, bounds.height() - 6, 0xFFD35F5F);
        };
        return new DemoIcons() {
            @Override
            public SlotIcon primary() {
                return primary;
            }

            @Override
            public SlotIcon secondary() {
                return secondary;
            }
        };
    }
}
