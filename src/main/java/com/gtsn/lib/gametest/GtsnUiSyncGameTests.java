package com.gtsn.lib.gametest;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.ui.demo.DemoMenu;
import com.gtsn.lib.ui.demo.DemoMenus;
import com.gtsn.lib.ui.demo.DemoPhase;
import com.gtsn.lib.ui.demo.DemoSync;
import com.gtsn.lib.ui.sync.MenuSync;
import com.gtsn.lib.ui.sync.bind.SyncBinding;
import com.gtsn.lib.ui.sync.bind.SyncBindings;
import com.gtsn.lib.ui.widget.ProgressBarWidget;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * UI 容器数据同步在专职服务端加载环境（runGameTestServer）中的行为验证：
 * 服务端写入 → 原版数据槽同步（{@code broadcastChanges} → {@code ContainerSynchronizer}）→
 * 客户端收包路径（{@code setData}）→ 绑定层更新控件。
 *
 * <p>使用真实 {@link AbstractContainerMenu} 与数据槽机制（{@link DemoMenu}），
 * 只以捕获型 {@link ContainerSynchronizer} 代替网络发送；绑定层为纯 Java。</p>
 */
@GameTestHolder(GTSNLib.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GtsnUiSyncGameTests {

    private GtsnUiSyncGameTests() {
    }

    /** 捕获同步调用的假同步器：代替客户端网络发送，断言服务端推了哪些数据槽。 */
    private static final class CapturingSynchronizer implements ContainerSynchronizer {

        private final List<String> dataChanges = new ArrayList<>();
        private int[] initialData;

        @Override
        public void sendInitialData(AbstractContainerMenu container, NonNullList<ItemStack> items,
                                    ItemStack carriedItem, int[] initialData) {
            this.initialData = initialData.clone();
        }

        @Override
        public void sendSlotChange(AbstractContainerMenu container, int slot, ItemStack itemStack) {
        }

        @Override
        public void sendCarriedChange(AbstractContainerMenu containerMenu, ItemStack stack) {
        }

        @Override
        public void sendDataChange(AbstractContainerMenu container, int id, int value) {
            dataChanges.add(id + "=" + value);
        }
    }

    private static DemoMenu serverMenu(GameTestHelper helper) {
        Player player = helper.makeMockPlayer();
        return new DemoMenu(DemoMenus.SYNC_DEMO.get(), 1, player.getInventory());
    }

    @GameTest(template = "empty")
    public static void defaultsArePublishedOnInitialSync(GameTestHelper helper) {
        DemoMenu menu = serverMenu(helper);
        CapturingSynchronizer synchronizer = new CapturingSynchronizer();

        menu.setSynchronizer(synchronizer);

        if (synchronizer.initialData == null) {
            helper.fail("initial data was not sent on synchronizer attach");
            return;
        }
        if (synchronizer.initialData.length != DemoSync.LAYOUT.slotCount()) {
            helper.fail("initial data length was " + synchronizer.initialData.length
                    + ", expected " + DemoSync.LAYOUT.slotCount());
            return;
        }
        if (synchronizer.initialData[DemoSync.PROGRESS.index()] != 0
                || synchronizer.initialData[DemoSync.SPEED.index()] != Float.floatToRawIntBits(1f)
                || synchronizer.initialData[DemoSync.ACTIVE.index()] != 1) {
            helper.fail("initial data did not contain the slot defaults");
            return;
        }
        if (menu.syncData().getCount() != DemoSync.LAYOUT.slotCount()) {
            helper.fail("registered data slot count was " + menu.syncData().getCount());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void serverWriteReachesRemoteThroughDataSlots(GameTestHelper helper) {
        DemoMenu menu = serverMenu(helper);
        CapturingSynchronizer synchronizer = new CapturingSynchronizer();
        menu.setSynchronizer(synchronizer);
        synchronizer.dataChanges.clear();

        menu.sync().set(DemoSync.PROGRESS, 42);
        menu.sync().set(DemoSync.PHASE, DemoPhase.RUNNING);
        menu.broadcastChanges();

        if (!synchronizer.dataChanges.contains(DemoSync.PROGRESS.index() + "=42")) {
            helper.fail("progress change was not sent: " + synchronizer.dataChanges);
            return;
        }
        if (!synchronizer.dataChanges.contains(DemoSync.PHASE.index() + "=" + DemoPhase.RUNNING.ordinal())) {
            helper.fail("phase change was not sent: " + synchronizer.dataChanges);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void clientMirrorReceivesDataAndDrivesBinding(GameTestHelper helper) {
        DemoMenu clientMenu = serverMenu(helper);
        MenuSync sync = clientMenu.sync();

        clientMenu.setData(DemoSync.PROGRESS.index(), 55); // 模拟 ClientboundContainerSetDataPacket 处理
        if (sync.get(DemoSync.PROGRESS) != 55) {
            helper.fail("client mirror did not read the received value: " + sync.get(DemoSync.PROGRESS));
            return;
        }
        int[] notifications = {0};
        sync.onChange(DemoSync.PROGRESS, (oldValue, newValue) -> notifications[0]++);
        int changed = sync.refresh();
        if (changed != 1 || notifications[0] != 1) {
            helper.fail("refresh changed=" + changed + " notifications=" + notifications[0]);
            return;
        }

        ProgressBarWidget bar = new ProgressBarWidget().range(0, 100);
        SyncBinding binding = SyncBindings.progressBar(sync, DemoSync.PROGRESS, bar);
        if (Math.abs(bar.value() - 55.0) > 1e-9) {
            helper.fail("binding did not apply the client value: " + bar.value());
            return;
        }

        clientMenu.setData(DemoSync.PROGRESS.index(), 70);
        sync.refresh();
        if (Math.abs(bar.value() - 70.0) > 1e-9) {
            helper.fail("binding did not follow the next packet: " + bar.value());
            return;
        }
        binding.close();
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void demoServerTicksAdvanceAndWrapThroughSynchronizer(GameTestHelper helper) {
        DemoMenu menu = serverMenu(helper);
        CapturingSynchronizer synchronizer = new CapturingSynchronizer();
        menu.setSynchronizer(synchronizer);

        for (int i = 0; i < DemoSync.PROGRESS_PERIOD_TICKS; i++) {
            menu.broadcastChanges();
        }
        if (menu.sync().get(DemoSync.PROGRESS) != DemoSync.PROGRESS_STEP) {
            helper.fail("progress after one period was " + menu.sync().get(DemoSync.PROGRESS));
            return;
        }
        if (menu.sync().get(DemoSync.PHASE) != DemoSync.phaseFor(DemoSync.PROGRESS_STEP)) {
            helper.fail("phase did not follow progress: " + menu.sync().get(DemoSync.PHASE));
            return;
        }

        // 推进到 100（DONE），再一个周期回卷到 0（IDLE）并轮换档位 / 速度 / 开关。
        for (int i = 0; i < DemoSync.PROGRESS_PERIOD_TICKS * 19; i++) {
            menu.broadcastChanges();
        }
        if (menu.sync().get(DemoSync.PROGRESS) != DemoSync.PROGRESS_MAX
                || menu.sync().get(DemoSync.PHASE) != DemoPhase.DONE) {
            helper.fail("progress did not reach max/done: progress=" + menu.sync().get(DemoSync.PROGRESS)
                    + " phase=" + menu.sync().get(DemoSync.PHASE));
            return;
        }
        for (int i = 0; i < DemoSync.PROGRESS_PERIOD_TICKS; i++) {
            menu.broadcastChanges();
        }
        int expectedTier = DemoSync.nextTier(0);
        if (menu.sync().get(DemoSync.PROGRESS) != DemoSync.PROGRESS_MIN
                || menu.sync().get(DemoSync.PHASE) != DemoPhase.IDLE
                || menu.sync().get(DemoSync.TIER) != expectedTier
                || Math.abs(menu.sync().get(DemoSync.SPEED) - DemoSync.speedForTier(expectedTier)) > 1e-6
                || menu.sync().get(DemoSync.ACTIVE) != (expectedTier % 2 == 0)) {
            helper.fail("wrap did not rotate demo values: progress=" + menu.sync().get(DemoSync.PROGRESS)
                    + " phase=" + menu.sync().get(DemoSync.PHASE) + " tier=" + menu.sync().get(DemoSync.TIER)
                    + " speed=" + menu.sync().get(DemoSync.SPEED) + " active=" + menu.sync().get(DemoSync.ACTIVE));
            return;
        }
        if (!synchronizer.dataChanges.contains(DemoSync.PROGRESS.index() + "=" + DemoSync.PROGRESS_STEP)) {
            helper.fail("advance was not pushed to the remote: " + synchronizer.dataChanges);
            return;
        }
        helper.succeed();
    }
}
