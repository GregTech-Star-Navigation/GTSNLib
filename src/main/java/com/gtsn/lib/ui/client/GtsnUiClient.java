package com.gtsn.lib.ui.client;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.gt.adapter.GtMachineSnapshots;
import com.gtsn.lib.ui.demo.DemoContent;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.screen.GtsnUiTestScreen;
import com.gtsn.lib.ui.screen.MachineStatusScreen;
import com.gtsn.lib.ui.theme.FontId;
import com.gtsn.lib.ui.theme.ThemeContext;
import com.gtsn.lib.ui.widget.Widget;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
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
 *   <li>开发自动测试（环境变量 {@code GTSNLIB_UI_AUTOTEST}）：
 *       <ul>
 *         <li>{@code =1}：客户端进入标题界面后自动打开开发测试界面，注入合成点击/字符输入，随后逐主题点击
 *             “主题”按钮并各抓一张截图（{@code run/screenshots/gtsnlib-ui-theme-<主题>.png}），
 *             再对字体样例文本抓“主题字体 / 原版字体”对比截图（{@code gtsnlib-ui-font-theme.png} /
 *             {@code gtsnlib-ui-font-vanilla.png}，#23），最后退出。</li>
 *         <li>{@code =sync}（#19）：创建/载入固定存档后在游戏内打开数据同步演示菜单，采样客户端/服务端同步值
 *             并抓图（{@code run/screenshots/gtsnlib-ui-sync-demo.png}），最后退出（见 {@link GtsnUiSyncAutotest}）。</li>
 *       </ul>
 *   </li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = GTSNLib.MOD_ID, bus = Bus.FORGE, value = Dist.CLIENT)
public final class GtsnUiClient {

    /** 自动测试开关：环境变量为 {@code 1}（组件库/主题）或 {@code sync}（数据同步演示）时启用。 */
    public static final String AUTOTEST_ENV = "GTSNLIB_UI_AUTOTEST";

    private static final String SCREENSHOT_PREFIX = "gtsnlib-ui-theme-";
    private static final String SCREENSHOT_SUFFIX = ".png";
    private static final String FONT_THEME_SCREENSHOT = "gtsnlib-ui-font-theme.png";
    private static final String FONT_VANILLA_SCREENSHOT = "gtsnlib-ui-font-vanilla.png";

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
        event.getDispatcher().register(Commands.literal("gtsnui")
                .executes(context -> {
                    openTestScreen();
                    return 1;
                })
                .then(Commands.literal("machine").executes(context -> {
                    openMachineScreen();
                    return 1;
                })));
        LOGGER.info("[GTSNLib] client command /gtsnui registered (subcommand: machine)");
    }

    /** 打开开发测试界面（可在任意客户端线程调用）。 */
    public static void openTestScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> minecraft.setScreen(new GtsnUiTestScreen()));
    }

    /**
     * {@code /gtsnui machine}：对玩家注视的 GT 机器打开只读机器状态界面（本地执行、不发往服务器）。
     * 客户端从镜像机器的 {@code @DescSynced} 同步值读取，无需网络包。
     */
    public static void openMachineScreen() {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            Minecraft client = Minecraft.getInstance();
            if (client.player == null || client.level == null) {
                return;
            }
            BlockPos pos = lookedAtGtMachine(client);
            if (pos == null) {
                client.player.displayClientMessage(
                        Component.literal("[GTSNLib] 请面向一台 GT 机器（8 格内）再执行 /gtsnui machine"), false);
                return;
            }
            client.setScreen(new MachineStatusScreen(pos));
        });
    }

    /** 玩家注视的方块坐标；非 GT 机器时返回 {@code null}。 */
    private static BlockPos lookedAtGtMachine(Minecraft minecraft) {
        Player player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return null;
        }
        HitResult hit = player.pick(8.0, 0.0F, false);
        if (hit instanceof BlockHitResult blockHit && blockHit.getType() == HitResult.Type.BLOCK
                && GtMachineSnapshots.isGtMachine(minecraft.level, blockHit.getBlockPos())) {
            return blockHit.getBlockPos();
        }
        return null;
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        String mode = System.getenv(AUTOTEST_ENV);
        Minecraft minecraft = Minecraft.getInstance();
        if (GtsnUiMachineAutotest.MODE.equals(mode)) {
            GtsnUiMachineAutotest.tick(minecraft);
            return;
        }
        if (GtsnUiSyncAutotest.MODE.equals(mode)) {
            GtsnUiSyncAutotest.tick(minecraft);
            return;
        }
        if (!"1".equals(mode)) {
            return;
        }
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
            grab(minecraft, themeScreenshotName());
            return;
        }
        if (screenTicks >= 80 && themeShots == 1) {
            themeShots = 2;
            click(screen, screen.demo().themeButton());
            return;
        }
        if (screenTicks >= 100 && themeShots == 2) {
            themeShots = 3;
            grab(minecraft, themeScreenshotName());
            return;
        }
        if (screenTicks >= 120 && themeShots == 3) {
            themeShots = 4;
            click(screen, screen.demo().themeButton());
            return;
        }
        if (screenTicks >= 140 && themeShots == 4) {
            themeShots = 5;
            grab(minecraft, themeScreenshotName());
            return;
        }
        // 字体证据（#23）：切回默认主题后，对同一段中英混排样例文本各抓一张“主题字体 / 原版字体”截图。
        if (screenTicks >= 160 && themeShots == 5) {
            themeShots = 6;
            click(screen, screen.demo().themeButton());
            return;
        }
        if (screenTicks >= 180 && themeShots == 6) {
            themeShots = 7;
            logAndVerifyEffectiveFont(minecraft);
            grab(minecraft, FONT_THEME_SCREENSHOT);
            return;
        }
        if (screenTicks >= 200 && themeShots == 7) {
            themeShots = 8;
            click(screen, screen.demo().fontCompareButton());
            return;
        }
        if (screenTicks >= 220 && themeShots == 8) {
            themeShots = 9;
            grab(minecraft, FONT_VANILLA_SCREENSHOT);
            return;
        }
        if (screenTicks >= 260 && !stopRequested) {
            stopRequested = true;
            LOGGER.info("[GTSNLib] autotest complete (themes captured: {}), stopping client", themeShots);
            minecraft.stop();
        }
    }

    private static String themeScreenshotName() {
        return SCREENSHOT_PREFIX + ThemeContext.activeId().path() + SCREENSHOT_SUFFIX;
    }

    /**
     * 正向验证（#23）：记录实际生效字体，并断言自定义字体真的换了字库
     * （{@link GtsnUiFontProbe} 比较同一样例在主题 / 原版下的宽度，必须不同）。
     * 断言失败会抛出异常，使自动测试无需肉眼比对截图即可捕获“静默回退原版”的回归。
     */
    private static void logAndVerifyEffectiveFont(Minecraft minecraft) {
        FontId requested = ThemeContext.active().textStyle().fontId();
        GtsnUiFontProbe.Result probe = GtsnUiFontProbe.verifyActiveTheme(minecraft.font);
        LOGGER.info("[GTSNLib] UI effective font = {} (requested={}, themeWidth={}, vanillaWidth={}, delta={})",
                probe.effective() == null ? FontId.VANILLA.location() + "(fallback)" : probe.effective().location(),
                requested.location(), probe.themeWidth(), probe.vanillaWidth(),
                probe.themeWidth() - probe.vanillaWidth());
    }

    private static void grab(Minecraft minecraft, String name) {
        LOGGER.info("[GTSNLib] autotest grabbing screenshot {} (theme={})", name, ThemeContext.activeId());
        Screenshot.grab(minecraft.gameDirectory, name, minecraft.getMainRenderTarget(),
                message -> LOGGER.info("[GTSNLib] autotest screenshot: {}", message.getString()));
    }

    /**
     * 固定自动测试窗口为 1280x720 + GUI 缩放 2（640x360 GUI 空间），保证组件库完整可见、文字清晰。
     */
    private static void prepareWindow(Minecraft minecraft) {
        GtsnUiAutotestWindow.prepare(minecraft, "autotest");
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
