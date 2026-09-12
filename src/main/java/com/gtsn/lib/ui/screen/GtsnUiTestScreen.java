package com.gtsn.lib.ui.screen;

import com.gtsn.lib.ui.demo.DemoContent;
import com.gtsn.lib.ui.demo.DemoState;
import com.gtsn.lib.ui.widget.TextMetrics;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;

/**
 * 内置开发测试界面：装配 {@link DemoContent}（布局/点击/键盘/锚定/裁剪演示），
 * 可在游戏内通过客户端命令 {@code /gtsnui} 打开（另见 {@code GTSNLIB_UI_AUTOTEST} 自动测试开关）。
 *
 * <p>客户端专用类：专职服务端不得加载（类加载纪律，见 ADR-0003/0004）。</p>
 */
public final class GtsnUiTestScreen extends GtsnScreen {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final DemoContent demo;
    private int renderedFrames;

    public GtsnUiTestScreen() {
        this(new DemoState());
    }

    public GtsnUiTestScreen(DemoState state) {
        this(DemoContent.build(state, fontMetrics()));
    }

    private GtsnUiTestScreen(DemoContent demo) {
        super(Component.literal("GTSN UI Kernel — dev test screen"), demo.root());
        this.demo = demo;
    }

    public DemoContent demo() {
        return demo;
    }

    public int renderedFrames() {
        return renderedFrames;
    }

    @Override
    protected void init() {
        super.init();
        LOGGER.info("[GTSNLib] UI test screen init: {}x{}", width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderedFrames++;
        if (renderedFrames == 1 || renderedFrames % 120 == 0) {
            LOGGER.info("[GTSNLib] UI test screen rendered: frame={} clicks={} typed={}",
                    renderedFrames, demo.state().clicks(), demo.state().typed());
        }
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
