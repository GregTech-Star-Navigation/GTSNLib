package com.gtsn.lib.ui.client;

import com.gtsn.lib.api.GtMachineSnapshot;
import com.gtsn.lib.gt.adapter.GtMachineSnapshots;
import com.gtsn.lib.ui.screen.MachineStatusScreen;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.level.storage.LevelStorageSource;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.UUID;

/**
 * 机器界面桥接的客户端自动测试（开发专用，{@code GTSNLIB_UI_AUTOTEST=machine} 启用）：
 *
 * <ol>
 *   <li>在标题界面创建 / 载入固定存档 {@value #LEVEL_NAME}（集成服务端）；</li>
 *   <li>服务端线程经 {@code setblock} 在玩家旁放置 {@value #MACHINE_ID}；</li>
 *   <li>等待客户端出现镜像机器快照后，经集成服务端执行 {@code /gtsnlib debug charge …} 注入
 *       {@value #CHARGE_EU} EU，再在客户端轮询 {@link GtMachineSnapshots#at} 直到
 *       {@code energyStored() > 0}（证明 {@code @DescSynced} 客户端读取，能量容量为 tier 派生值，
 *       不能作为证据），超时即 FAIL；</li>
 *   <li>打开 {@link MachineStatusScreen}，校验界面快照的机器 id / tier / 非零能量，抓取截图
 *       {@code run/screenshots/}{@value #SCREENSHOT_NAME} 并退出客户端。</li>
 * </ol>
 *
 * <p>任何阶段超时都记录 FAIL 证据并退出，保证无人值守运行可终止。仅客户端加载。</p>
 */
final class GtsnUiMachineAutotest {

    /** 环境变量值（{@code GTSNLIB_UI_AUTOTEST=machine}）。 */
    static final String MODE = "machine";

    private static final String LEVEL_NAME = "gtsnlib-machine-autotest";
    private static final String SCREENSHOT_NAME = "gtsnlib-ui-machine-status.png";
    private static final String MACHINE_ID = "gtsnlib:test_machine";
    /** 服务端注入能量，保证客户端读到非零同步值（容量 2048，足够容纳）。 */
    private static final int CHARGE_EU = 512;
    /** 注入前先把服务端能量排空（远大于容量即可钳制到 0），保证 0 → N 是真实同步跃迁。 */
    private static final int DRAIN_EU = -1_000_000;
    private static final int WORLD_TIMEOUT_TICKS = 3600;
    private static final int STAGE_TIMEOUT_TICKS = 400;
    /** 客户端等待同步值跃迁的上限（tick）；超时即判定未收到同步值。 */
    private static final int ENERGY_SYNC_WAIT_TICKS = 100;

    private static final Logger LOGGER = LogUtils.getLogger();

    private enum Stage {
        TITLE, WORLD, PLACED, RESET, SYNC, SCREEN, GRAB, DONE, FAILED
    }

    private static Stage stage = Stage.TITLE;
    private static int ticks;
    private static boolean stopped;
    private static BlockPos target;

    private GtsnUiMachineAutotest() {
    }

    static void tick(Minecraft minecraft) {
        ticks++;
        switch (stage) {
            case TITLE -> tickTitle(minecraft);
            case WORLD -> tickWorld(minecraft);
            case PLACED -> tickPlaced(minecraft);
            case RESET -> tickReset(minecraft);
            case SYNC -> tickSync(minecraft);
            case SCREEN -> tickScreen(minecraft);
            case GRAB -> tickGrab(minecraft);
            case DONE, FAILED -> tickStop(minecraft);
        }
    }

    private static void tickTitle(Minecraft minecraft) {
        if (!minecraft.isRunning() || minecraft.getOverlay() != null
                || !(minecraft.screen instanceof TitleScreen)) {
            return;
        }
        GtsnUiAutotestWindow.prepare(minecraft, "machine autotest");
        loadOrCreateWorld(minecraft);
        stage = Stage.WORLD;
        ticks = 0;
    }

    private static void tickWorld(Minecraft minecraft) {
        if (minecraft.player != null && minecraft.getSingleplayerServer() != null
                && serverPlayer(minecraft) != null) {
            target = minecraft.player.blockPosition().offset(2, 0, 0);
            placeMachine(minecraft, target);
            LOGGER.info("[GTSNLib] machine autotest: placing {} at {}", MACHINE_ID, target);
            stage = Stage.PLACED;
            ticks = 0;
            return;
        }
        if (ticks > WORLD_TIMEOUT_TICKS) {
            fail(minecraft, "timed out waiting for the integrated world (ticks=" + ticks + ")");
        }
    }

    private static void tickPlaced(Minecraft minecraft) {
        Level level = minecraft.level;
        Optional<GtMachineSnapshot> resolved = level == null
                ? Optional.empty()
                : GtMachineSnapshots.at(level, target);
        if (resolved.isPresent()) {
            LOGGER.info("[GTSNLib] machine autotest: client mirror present (energy={}/{}), draining server-side to 0",
                    resolved.get().energyStored(), resolved.get().energyCapacity());
            chargeMachine(minecraft, target, DRAIN_EU);
            stage = Stage.RESET;
            ticks = 0;
            return;
        }
        if (ticks > STAGE_TIMEOUT_TICKS) {
            fail(minecraft, "client-side machine snapshot never appeared at " + target);
        }
    }

    /** 等待客户端镜像观察到排空后的 0 能量（可重复：清除复用存档里的历史值）。 */
    private static void tickReset(Minecraft minecraft) {
        Level level = minecraft.level;
        Optional<GtMachineSnapshot> resolved = level == null
                ? Optional.empty()
                : GtMachineSnapshots.at(level, target);
        if (resolved.isPresent() && resolved.get().energyCapacity() > 0L && resolved.get().energyStored() == 0L) {
            LOGGER.info("[GTSNLib] machine autotest: client mirror reset to 0 EU, charging {} EU server-side", CHARGE_EU);
            chargeMachine(minecraft, target, CHARGE_EU);
            stage = Stage.SYNC;
            ticks = 0;
            return;
        }
        if (ticks > ENERGY_SYNC_WAIT_TICKS) {
            fail(minecraft, "client mirror never reset to 0 EU at " + target + " (client energyStored="
                    + resolved.map(GtMachineSnapshot::energyStored).map(Object::toString).orElse("<no snapshot>") + ")");
        }
    }

    /**
     * 等待客户端从 LDLib {@code @DescSynced} 镜像读到非零能量。能量容量（2048）是 tier 派生值、两端一致，
     * 只有这个「服务端注入后客户端从 0 变为非零」的跃迁才真正证明同步链路——收不到即 FAIL。
     */
    private static void tickSync(Minecraft minecraft) {
        Level level = minecraft.level;
        Optional<GtMachineSnapshot> resolved = level == null
                ? Optional.empty()
                : GtMachineSnapshots.at(level, target);
        if (resolved.isPresent() && resolved.get().energyStored() > 0L) {
            GtMachineSnapshot snapshot = resolved.get();
            LOGGER.info("[GTSNLib] machine autotest: client received SYNCED energy {}/{} (id={} tier={} status={})",
                    snapshot.energyStored(), snapshot.energyCapacity(), snapshot.machineId(),
                    snapshot.tier(), snapshot.status());
            minecraft.setScreen(new MachineStatusScreen(target));
            stage = Stage.SCREEN;
            ticks = 0;
            return;
        }
        if (ticks > ENERGY_SYNC_WAIT_TICKS) {
            fail(minecraft, "client never received the synced energy value at " + target
                    + " (server injected " + CHARGE_EU + " EU after reset; client energyStored="
                    + resolved.map(GtMachineSnapshot::energyStored).map(Object::toString).orElse("<no snapshot>")
                    + ") -- @DescSynced mirror read NOT proven");
        }
    }

    private static void tickScreen(Minecraft minecraft) {
        if (!(minecraft.screen instanceof MachineStatusScreen screen)) {
            if (ticks > STAGE_TIMEOUT_TICKS) {
                fail(minecraft, "machine status screen did not open; current screen=" + minecraft.screen);
            }
            return;
        }
        GtMachineSnapshot snapshot = screen.snapshot();
        if (!MACHINE_ID.equals(snapshot.machineId())) {
            fail(minecraft, "screen snapshot machine id was " + snapshot.machineId());
            return;
        }
        if (snapshot.tier() != 1 || snapshot.energyCapacity() <= 0L || snapshot.energyStored() <= 0L) {
            if (ticks > STAGE_TIMEOUT_TICKS) {
                fail(minecraft, "screen snapshot incomplete: tier=" + snapshot.tier()
                        + " energyStored=" + snapshot.energyStored()
                        + " energyCapacity=" + snapshot.energyCapacity());
            }
            return;
        }
        LOGGER.info("[GTSNLib] machine autotest PASS: screen shows {} tier={} energy={}/{} status={}",
                snapshot.machineId(), snapshot.tier(), snapshot.energyStored(),
                snapshot.energyCapacity(), snapshot.status());
        stage = Stage.GRAB;
        ticks = 0;
    }

    private static void tickGrab(Minecraft minecraft) {
        if (ticks < 10) {
            return;
        }
        GtsnUiAutotestWindow.ensureSized(minecraft, "machine autotest");
        LOGGER.info("[GTSNLib] machine autotest grabbing screenshot {} (window={}x{} target={}x{})",
                SCREENSHOT_NAME, minecraft.getWindow().getWidth(), minecraft.getWindow().getHeight(),
                minecraft.getMainRenderTarget().width, minecraft.getMainRenderTarget().height);
        grabScreenshot(minecraft);
        stage = Stage.DONE;
        ticks = 0;
    }

    private static void tickStop(Minecraft minecraft) {
        if (ticks > 40 && !stopped) {
            stopped = true;
            LOGGER.info("[GTSNLib] machine autotest finished ({}), stopping client", stage);
            minecraft.stop();
        }
    }

    private static void fail(Minecraft minecraft, String reason) {
        LOGGER.error("[GTSNLib] machine autotest FAIL: {}", reason);
        grabScreenshot(minecraft);
        stage = Stage.FAILED;
        ticks = 0;
    }

    private static void grabScreenshot(Minecraft minecraft) {
        Screenshot.grab(minecraft.gameDirectory, SCREENSHOT_NAME, minecraft.getMainRenderTarget(),
                message -> LOGGER.info("[GTSNLib] machine autotest screenshot: {}", message.getString()));
    }

    private static ServerPlayer serverPlayer(Minecraft minecraft) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) {
            return null;
        }
        return server.getPlayerList().getPlayer(minecraft.player.getUUID());
    }

    private static void placeMachine(Minecraft minecraft, BlockPos target) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            return;
        }
        UUID uuid = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                return;
            }
            String command = "setblock " + target.getX() + " " + target.getY() + " " + target.getZ()
                    + " " + MACHINE_ID;
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(), command);
        });
    }

    /**
     * 服务端线程执行 {@code /gtsnlib debug charge <x> <y> <z> <eu>}，向机器能量容器注入能量，
     * 作为「服务端权威写」的一侧；客户端随后以镜像读到非零值为同步证据。
     */
    private static void chargeMachine(Minecraft minecraft, BlockPos target, int eu) {
        IntegratedServer server = minecraft.getSingleplayerServer();
        if (server == null) {
            return;
        }
        UUID uuid = minecraft.player.getUUID();
        server.execute(() -> {
            ServerPlayer player = server.getPlayerList().getPlayer(uuid);
            if (player == null) {
                return;
            }
            String command = "gtsnlib debug charge " + target.getX() + " " + target.getY() + " "
                    + target.getZ() + " " + eu;
            LOGGER.info("[GTSNLib] machine autotest: server executing '{}'", command);
            server.getCommands().performPrefixedCommand(
                    server.createCommandSourceStack().withSuppressedOutput(), command);
        });
    }

    /**
     * 创建 / 载入固定存档（首次创建，之后复用，自动测试可重复）。
     */
    private static void loadOrCreateWorld(Minecraft minecraft) {
        LevelStorageSource levelSource = minecraft.getLevelSource();
        if (levelSource.levelExists(LEVEL_NAME)) {
            LOGGER.info("[GTSNLib] machine autotest: loading existing world '{}'", LEVEL_NAME);
            minecraft.createWorldOpenFlows().loadLevel(new TitleScreen(), LEVEL_NAME);
            return;
        }
        LOGGER.info("[GTSNLib] machine autotest: creating world '{}'", LEVEL_NAME);
        LevelSettings settings = new LevelSettings(LEVEL_NAME, GameType.CREATIVE, false, Difficulty.PEACEFUL,
                true, new GameRules(), WorldDataConfiguration.DEFAULT);
        minecraft.createWorldOpenFlows().createFreshLevel(LEVEL_NAME, settings,
                WorldOptions.defaultWithRandomSeed(), WorldPresets::createNormalWorldDimensions);
    }
}
