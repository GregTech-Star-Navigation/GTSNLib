package com.gtsn.lib.ui.client;

import com.gtsn.lib.ui.demo.DemoMenu;
import com.gtsn.lib.ui.demo.DemoSync;
import com.gtsn.lib.ui.screen.DemoMenuScreen;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import net.minecraftforge.network.NetworkHooks;
import org.slf4j.Logger;

import java.util.UUID;

/**
 * 数据同步演示的客户端自动测试（开发专用，{@code GTSNLIB_UI_AUTOTEST=sync} 启用）：
 *
 * <ol>
 *   <li>在标题界面创建 / 载入固定存档 {@value #LEVEL_NAME}（集成服务端）；</li>
 *   <li>经服务端线程用 {@link NetworkHooks#openScreen} 打开 {@link DemoMenu}；</li>
 *   <li>等待 {@link DemoMenuScreen} 出现，采样两次「客户端菜单值 / 服务端菜单值 / 屏幕文本」，
 *       要求两次采样值不同（界面在更新）且每次客户端值等于服务端值；</li>
 *   <li>抓取截图 {@code run/screenshots/}{@value #SCREENSHOT_NAME} 并退出客户端。</li>
 * </ol>
 *
 * <p>任何阶段超时都记录 FAIL 证据并退出，保证无人值守运行可终止。仅客户端加载。</p>
 */
final class GtsnUiSyncAutotest {

    /** 环境变量值（{@code GTSNLIB_UI_AUTOTEST=sync}）。 */
    static final String MODE = "sync";

    private static final String LEVEL_NAME = "gtsnlib-sync-autotest";
    private static final String SCREENSHOT_NAME = "gtsnlib-ui-sync-demo.png";
    private static final int WORLD_TIMEOUT_TICKS = 3600;
    private static final int STAGE_TIMEOUT_TICKS = 400;
    /** 两次采样之间的等待 tick（服务端每 10 tick 推进一次，40 tick ≈ 走 4 步）。 */
    private static final int SAMPLE_GAP_TICKS = 40;

    private static final Logger LOGGER = LogUtils.getLogger();

    private enum Stage {
        TITLE, WORLD, SCREEN, SAMPLE_A, SAMPLE_B, GRAB, DONE, FAILED
    }

    private static Stage stage = Stage.TITLE;
    private static int ticks;
    private static boolean stopped;
    private static int sampleA = -1;
    private static int sampleB = -1;

    /** 服务端线程写入的最新服务端进度（volatile：客户端线程读取）。 */
    private static volatile int latestServerProgress = Integer.MIN_VALUE;
    /** 距离最近一次成功服务端读取的 tick 数。 */
    private static volatile int serverReadAge = Integer.MAX_VALUE;

    private GtsnUiSyncAutotest() {
    }

    static void tick(Minecraft minecraft) {
        ticks++;
        switch (stage) {
            case TITLE -> tickTitle(minecraft);
            case WORLD -> tickWorld(minecraft);
            case SCREEN -> tickScreen(minecraft);
            case SAMPLE_A -> tickSample(minecraft, true);
            case SAMPLE_B -> tickSample(minecraft, false);
            case GRAB -> tickGrab(minecraft);
            case DONE, FAILED -> tickStop(minecraft);
        }
    }

    private static void tickTitle(Minecraft minecraft) {
        if (!minecraft.isRunning() || minecraft.getOverlay() != null
                || !(minecraft.screen instanceof TitleScreen)) {
            return;
        }
        prepareWindow(minecraft);
        loadOrCreateWorld(minecraft);
        stage = Stage.WORLD;
        ticks = 0;
    }

    private static void tickWorld(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.getSingleplayerServer() != null
                && serverPlayer(minecraft) != null) {
            openDemoMenu(minecraft);
            stage = Stage.SCREEN;
            ticks = 0;
            LOGGER.info("[GTSNLib] sync autotest: world ready, opening demo menu");
            return;
        }
        if (ticks > WORLD_TIMEOUT_TICKS) {
            fail(minecraft, "timed out waiting for the integrated world (ticks=" + ticks + ")");
        }
    }

    private static void tickScreen(Minecraft minecraft) {
        pollServerProgress(minecraft);
        if (minecraft.screen instanceof DemoMenuScreen screen) {
            stage = Stage.SAMPLE_A;
            ticks = 0;
            LOGGER.info("[GTSNLib] sync autotest: demo screen open (slots={})", DemoSync.LAYOUT.slotCount());
            return;
        }
        if (ticks > STAGE_TIMEOUT_TICKS) {
            fail(minecraft, "demo screen did not open; current screen=" + minecraft.screen);
        }
    }

    private static void tickSample(Minecraft minecraft, boolean first) {
        if (!(minecraft.screen instanceof DemoMenuScreen screen)) {
            fail(minecraft, "demo screen closed before sampling completed");
            return;
        }
        pollServerProgress(minecraft);
        int minimumTicks = first ? 20 : SAMPLE_GAP_TICKS;
        if (ticks < minimumTicks) {
            return;
        }
        int clientValue = screen.getMenu().sync().get(DemoSync.PROGRESS);
        if (serverReadAge <= 3 && clientValue == latestServerProgress) {
            if (first) {
                sampleA = clientValue;
                LOGGER.info("[GTSNLib] sync autotest sample A: client={} server={} display='{}'",
                        clientValue, latestServerProgress, screen.content().progressLabel().text());
                stage = Stage.SAMPLE_B;
                ticks = 0;
            } else {
                sampleB = clientValue;
                finish(minecraft, screen);
            }
            return;
        }
        if (ticks > STAGE_TIMEOUT_TICKS) {
            fail(minecraft, "sample timeout: client=" + clientValue + " server=" + latestServerProgress
                    + " serverReadAge=" + serverReadAge);
        }
    }

    private static void finish(Minecraft minecraft, DemoMenuScreen screen) {
        String label = screen.content().progressLabel().text();
        boolean updated = sampleB != sampleA;
        boolean matches = sampleB == latestServerProgress;
        // 精确比对完整进度文本，避免 "3" 误匹配 "30 / 100" 这类子串弱断言。
        String expectedLabel = "进度 " + sampleB + " / " + DemoSync.PROGRESS_MAX;
        boolean labelShowsValue = label.equals(expectedLabel);
        boolean progressBarShowsValue = Math.abs(screen.content().progressBar().value() - sampleB) < 1e-9;
        LOGGER.info("[GTSNLib] sync autotest sample B: client={} server={} display='{}'",
                sampleB, latestServerProgress, label);
        if (!updated || !matches || !labelShowsValue || !progressBarShowsValue) {
            fail(minecraft, "verification failed: updated=" + updated + " matches=" + matches
                    + " labelShowsValue=" + labelShowsValue + " progressBarShowsValue=" + progressBarShowsValue);
            return;
        }
        LOGGER.info("[GTSNLib] sync autotest PASS: client value advanced {} -> {}; equals server {}; display='{}'",
                sampleA, sampleB, latestServerProgress, label);
        ensureWindowSized(minecraft);
        stage = Stage.GRAB;
        ticks = 0;
    }

    /** 截图阶段：等待若干 tick（窗口尺寸修正后重新渲染），再抓图并记录渲染目标尺寸。 */
    private static void tickGrab(Minecraft minecraft) {
        if (ticks < 10) {
            return;
        }
        LOGGER.info("[GTSNLib] sync autotest grabbing screenshot {} (window={}x{} target={}x{})",
                SCREENSHOT_NAME, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(),
                minecraft.getMainRenderTarget().width, minecraft.getMainRenderTarget().height);
        grabScreenshot(minecraft);
        stage = Stage.DONE;
        ticks = 0;
    }

    private static void tickStop(Minecraft minecraft) {
        if (ticks > 40 && !stopped) {
            stopped = true;
            LOGGER.info("[GTSNLib] sync autotest finished ({}), stopping client", stage);
            minecraft.stop();
        }
    }

    private static void fail(Minecraft minecraft, String reason) {
        LOGGER.error("[GTSNLib] sync autotest FAIL: {}", reason);
        grabScreenshot(minecraft);
        stage = Stage.FAILED;
        ticks = 0;
    }

    private static void grabScreenshot(Minecraft minecraft) {
        Screenshot.grab(minecraft.gameDirectory, SCREENSHOT_NAME, minecraft.getMainRenderTarget(),
                message -> LOGGER.info("[GTSNLib] sync autotest screenshot: {}", message.getString()));
    }

    private static void pollServerProgress(Minecraft minecraft) {
        serverReadAge++;
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) {
            return;
        }
        UUID uuid = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null && player.containerMenu instanceof DemoMenu menu) {
                latestServerProgress = menu.sync().get(DemoSync.PROGRESS);
                serverReadAge = 0;
            }
        });
    }

    private static ServerPlayer serverPlayer(Minecraft minecraft) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(minecraft.player.getUUID());
    }

    private static void openDemoMenu(Minecraft minecraft) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) {
            return;
        }
        UUID uuid = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player != null) {
                NetworkHooks.openScreen(player, DemoMenu.provider());
            }
        });
    }

    /**
     * 创建 / 载入固定存档（首次创建，之后复用，自动测试可重复）。
     * 存档创建经 {@link net.minecraft.client.gui.screens.worldselection.WorldOpenFlows} 的公开入口，
     * 不依赖世界创建界面操作。
     */
    private static void loadOrCreateWorld(Minecraft minecraft) {
        LevelStorageSource levelSource = minecraft.getLevelSource();
        if (levelSource.levelExists(LEVEL_NAME)) {
            LOGGER.info("[GTSNLib] sync autotest: loading existing world '{}'", LEVEL_NAME);
            minecraft.createWorldOpenFlows().loadLevel(new TitleScreen(), LEVEL_NAME);
            return;
        }
        LOGGER.info("[GTSNLib] sync autotest: creating world '{}'", LEVEL_NAME);
        LevelSettings settings = new LevelSettings(LEVEL_NAME, GameType.CREATIVE, false, Difficulty.PEACEFUL,
                true, new GameRules(), WorldDataConfiguration.DEFAULT);
        minecraft.createWorldOpenFlows().createFreshLevel(LEVEL_NAME, settings,
                WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions);
    }

    /** 固定窗口 1280x720 + GUI 缩放 2，保证截图清晰（与组件库自动测试共用窗口准备）。 */
    private static void prepareWindow(Minecraft minecraft) {
        GtsnUiAutotestWindow.prepare(minecraft, "sync autotest");
    }

    /** 截图前确保窗口尺寸（窗口被最小化 / 桌面会话变化时重新恢复并应用尺寸）。 */
    private static void ensureWindowSized(Minecraft minecraft) {
        GtsnUiAutotestWindow.ensureSized(minecraft, "sync autotest");
    }
}
