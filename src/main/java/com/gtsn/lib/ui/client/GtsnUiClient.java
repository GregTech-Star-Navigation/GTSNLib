package com.gtsn.lib.ui.client;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.ui.demo.DemoContent;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.screen.GtsnUiTestScreen;
import com.gtsn.lib.ui.theme.ThemeContext;
import com.gtsn.lib.ui.widget.Widget;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.Commands;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.slf4j.Logger;

/**
 * UI 内核的客户端接线（仅客户端加载，由 {@code @EventBusSubscriber(Dist.CLIENT)} 保证专职服务端不加载本类）：
 *
 * <ul>
 *   <li>注册客户端命令 {@code /gtsnui}（经 Forge {@link RegisterClientCommandsEvent}，本地执行、不发往服务器）。</li>
 *   <li>开发自动测试：设置环境变量 {@code GTSNLIB_UI_AUTOTEST=1} 后，客户端进入标题界面时自动打开
 *       开发测试界面，注入合成点击/字符输入，随后逐主题点击“主题”按钮并各抓一张截图
 *       （{@code run/screenshots/gtsnlib-ui-theme-<主题>.png}），最后自动退出客户端。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GTSNLib.MOD_ID, bus = Bus.FORGE, value = Dist.CLIENT)
public final class GtsnUiClient {

    /** 自动测试开关：环境变量值为 {@code 1} 时启用（runClient 继承进程环境变量）。 */
    public static final String AUTOTEST_ENV = "GTSNLIB_UI_AUTOTEST";

    private static final String SCREENSHOT_PREFIX = "gtsnlib-ui-theme-";
    private static final String SCREENSHOT_SUFFIX = ".png";

    private static final Logger LOGGER = LogUtils.getLogger();

    private static boolean autoOpened;
    private static int screenTicks;
    private static boolean inputInjected;
    private static int themeShots;
    private static boolean stopRequested;

    private GtsnUiClient() {
    }

    @SubscribeEvent
    public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("gtsnui").executes(context -> {
            openTestScreen();
            return 1;
        }));
        LOGGER.info("[GTSNLib] client command /gtsnui registered");
    }

    /** 打开开发测试界面（可在任意客户端线程调用）。 */
    public static void openTestScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.setScreen(new GtsnUiTestScreen()));
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !"1".equals(System.getenv(AUTOTEST_ENV))) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof GtsnUiTestScreen testScreen) {
            tickAutotest(minecraft, testScreen);
            return;
        }
        if (!autoOpened && minecraft.isRunning() && minecraft.screen instanceof TitleScreen
                && minecraft.getOverlay() == null) {
            autoOpened = true;
            prepareWindow(minecraft);
            LOGGER.info("[GTSNLib] {}={} -> opening UI dev test screen", AUTOTEST_ENV, System.getenv(AUTOTEST_ENV));
            minecraft.setScreen(new GtsnUiTestScreen());
        }
    }

    private static void tickAutotest(Minecraft minecraft, GtsnUiTestScreen screen) {
        screenTicks++;
        if (screenTicks >= 40 && !inputInjected) {
            inputInjected = true;
            injectSyntheticInput(screen);
            return;
        }
        // 每个主题一张截图：默认 → 点击切换 → 下一主题 → 再切换 → 第三个主题。
        if (screenTicks >= 60 && themeShots == 0) {
            themeShots = 1;
            grab(minecraft);
            return;
        }
        if (screenTicks >= 80 && themeShots == 1) {
            themeShots = 2;
            click(screen, screen.demo().themeButton());
            return;
        }
        if (screenTicks >= 100 && themeShots == 2) {
            themeShots = 3;
            grab(minecraft);
            return;
        }
        if (screenTicks >= 120 && themeShots == 3) {
            themeShots = 4;
            click(screen, screen.demo().themeButton());
            return;
        }
        if (screenTicks >= 140 && themeShots == 4) {
            themeShots = 5;
            grab(minecraft);
            return;
        }
        if (screenTicks >= 200 && !stopRequested) {
            stopRequested = true;
            LOGGER.info("[GTSNLib] autotest complete (themes captured: {}), stopping client", themeShots);
            minecraft.stop();
        }
    }

    private static void grab(Minecraft minecraft) {
        String name = SCREENSHOT_PREFIX + ThemeContext.activeId().path() + SCREENSHOT_SUFFIX;
        LOGGER.info("[GTSNLib] autotest grabbing screenshot {} (theme={})", name, ThemeContext.activeId());
        Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(),
                message -> LOGGER.info("[GTSNLib] autotest screenshot: {}", message.getString()));
    }

    /**
     * 固定自动测试窗口为 1280x720 + GUI 缩放 2（640x360 GUI 空间），保证组件库完整可见、文字清晰。
     */
    private static void prepareWindow(Minecraft minecraft) {
        minecraft.getWindow().setWindowed(1280, 720);
        minecraft.options.guiScale().set(2);
        minecraft.resizeDisplay();
        LOGGER.info("[GTSNLib] autotest window prepared: {}x{} guiScale=2",
                minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight());
    }

    /**
     * 通过 {@link GtsnUiTestScreen} 的真实输入入口注入合成事件，覆盖全部组件交互：
     * 按钮点击 / 复选框与开关切换 / 进度推进 / 物品槽选择 / 滚轮滚动 / 键盘输入 / 禁用控件 / 工具提示悬停。
     */
    private static void injectSyntheticInput(GtsnUiTestScreen screen) {
        DemoContent demo = screen.demo();
        click(screen, demo.clickButton());
        click(screen, demo.checkbox());
        click(screen, demo.toggleSwitch());
        click(screen, demo.progressUpButton());
        click(screen, demo.progressUpButton());
        click(screen, demo.progressUpButton());
        click(screen, demo.itemSlot1());
        click(screen, demo.disabledButton());

        Rect viewport = demo.scrollPanel().viewportBounds();
        screen.mouseScrolled(viewport.x() + 8, viewport.y() + 8, -2.0);

        click(screen, demo.keypad());
        screen.keyPressed('K', 0, 0);
        screen.charTyped('A', 0);

        // 最后悬停物品槽，让工具提示出现在截图中。
        Rect slot = demo.itemSlot1().bounds();
        screen.mouseMoved(slot.x() + slot.width() / 2.0, slot.y() + slot.height() / 2.0);

        LOGGER.info("[GTSNLib] autotest synthetic input: clicks={} toggled={} checkbox={} switch={} progress={} "
                        + "slot1={} scrollY={} lastKey={} typed={}",
                demo.state().clicks(), demo.state().toggled(), demo.checkbox().checked(),
                demo.toggleSwitch().checked(), demo.state().progress(), demo.itemSlot1().isSelected(),
                demo.scrollPanel().scrollY(), demo.state().lastKey(), demo.state().typed());
    }

    private static void click(GtsnUiTestScreen screen, Widget widget) {
        Rect bounds = widget.bounds();
        double x = bounds.x() + bounds.width() / 2.0;
        double y = bounds.y() + bounds.height() / 2.0;
        screen.mouseClicked(x, y, 0);
        screen.mouseReleased(x, y, 0);
    }
}
