package com.gtsn.lib.ui.screen;

import com.gtsn.lib.ui.demo.DemoMenu;
import com.gtsn.lib.ui.demo.DemoSync;
import com.gtsn.lib.ui.demo.SyncDemoContent;
import com.gtsn.lib.ui.sync.bind.SyncBindingGroup;
import com.gtsn.lib.ui.widget.TextMetrics;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.slf4j.Logger;

/**
 * 数据同步演示屏幕：展示 {@link SyncDemoContent}（进度条 + 各类型值文本），
 * 每 tick 经 {@link SyncBindingGroup#refresh()} 检测数据槽变化并更新控件。
 *
 * <p>由 {@code DemoMenuScreen(DemoMenu, Inventory, Component)} 经 {@code MenuScreens} 创建
 * （见 {@code com.gtsn.lib.ui.client.GtsnUiScreens}）。客户端专用类。</p>
 */
public final class DemoMenuScreen extends GtsnScreen implements MenuAccess<DemoMenu> {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final DemoMenu menu;
    private final SyncDemoContent content;
    private final SyncBindingGroup bindings;
    private int renderedFrames;

    public DemoMenuScreen(DemoMenu menu, Inventory inventory, Component title) {
        this(menu, SyncDemoContent.build(fontMetrics()));
    }

    private DemoMenuScreen(DemoMenu menu, SyncDemoContent content) {
        super(Component.literal("GTSN UI Sync Demo"), content.root());
        this.menu = menu;
        this.content = content;
        this.bindings = content.bind(menu.sync());
        LOGGER.info("[GTSNLib] sync demo screen opened: slots={} progress={}",
                DemoSync.LAYOUT.slotCount(), menu.sync().get(DemoSync.PROGRESS));
    }

    @Override
    public DemoMenu getMenu() {
        return menu;
    }

    /** 演示内容（自动测试读取控件状态用）。 */
    public SyncDemoContent content() {
        return content;
    }

    /** 已渲染帧数（诊断用）。 */
    public int renderedFrames() {
        return renderedFrames;
    }

    @Override
    public void tick() {
        super.tick();
        int changed = bindings.refresh();
        if (changed > 0) {
            LOGGER.info("[GTSNLib] sync demo applied {} slot change(s): progress={} phase={} tier={} active={}",
                    changed, menu.sync().get(DemoSync.PROGRESS), menu.sync().get(DemoSync.PHASE),
                    menu.sync().get(DemoSync.TIER), menu.sync().get(DemoSync.ACTIVE));
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderedFrames++;
    }

    @Override
    public void removed() {
        bindings.close();
        super.removed();
    }

    private static TextMetrics fontMetrics() {
        return new TextMetrics() {
            @Override
            public int width(String text) {
                return Minecraft.getInstance().font.width(text);
            }

            @Override
            public int lineHeight() {
                return Minecraft.getInstance().font.lineHeight;
            }
        };
    }
}
