package com.gtsn.lib.gametest;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.api.GtMachineSnapshot;
import com.gtsn.lib.gt.adapter.GtMachineDebug;
import com.gtsn.lib.gt.adapter.GtMachineSnapshots;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.Optional;

/**
 * 机器界面桥接的 GameTest（#22）：在真实加载环境中放置已注册的 {@code gtsnlib:test_machine}，
 * 经 {@link GtMachineSnapshots} 取只读快照，并断言**可失败**的具体值。
 *
 * <p>断言纪律（#22 评审 F1-2）：不再使用 {@code >= 0} / 「非空」这类因 {@link GtMachineSnapshot}
 * 钳制与 status 归一而永不失败的断言。改为：精确 {@code machineId} / {@code tier} / 容量 / 输入电压、
 * 无配方逻辑机器应为 {@code UNKNOWN} 且进度为 0，以及**服务端权威注入后 {@code energyStored} 精确等于注入量**
 * （该断言在未注入时确实失败，见验收证据的 RED）。</p>
 */
@GameTestHolder(GTSNLib.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GtsnMachineBridgeGameTests {

    private static final String MACHINE_ID = "gtsnlib:test_machine";
    /** {@code TieredEnergyMachine} tier 1 的派生容量（GTValues.V[1]*64*amperage）。 */
    private static final long EXPECTED_CAPACITY = 2048L;
    /** tier 1 的输入电压（GTValues.V[1]）。 */
    private static final long EXPECTED_INPUT_VOLTAGE = 32L;
    /** 服务端注入量；{@code test_machine} 为 LV 接收容器，容量 2048，足够容纳。 */
    private static final long INJECT_EU = 512L;

    private GtsnMachineBridgeGameTests() {
    }

    @GameTest(template = "empty")
    public static void snapshotReadsPlacedGtMachine(GameTestHelper helper) {
        Block machineBlock = BuiltInRegistries.BLOCK.get(new ResourceLocation("gtsnlib", "test_machine"));
        if (machineBlock == Blocks.AIR) {
            helper.fail("gtsnlib:test_machine block is not registered (demo content disabled?)");
            return;
        }

        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, machineBlock.defaultBlockState());
        BlockPos absolute = helper.absolutePos(relative);

        Optional<GtMachineSnapshot> resolved = GtMachineSnapshots.at(helper.getLevel(), absolute);
        if (resolved.isEmpty()) {
            helper.fail("no GT machine snapshot at placed gtsnlib:test_machine " + absolute);
            return;
        }
        GtMachineSnapshot before = resolved.get();

        if (!MACHINE_ID.equals(before.machineId())) {
            helper.fail("machine id was " + before.machineId() + ", expected " + MACHINE_ID);
            return;
        }
        if (before.tier() != 1) {
            helper.fail("machine tier was " + before.tier() + ", expected 1 (test machine)");
            return;
        }
        if (before.energyCapacity() != EXPECTED_CAPACITY) {
            helper.fail("energy capacity was " + before.energyCapacity() + ", expected " + EXPECTED_CAPACITY);
            return;
        }
        if (before.inputVoltage() != EXPECTED_INPUT_VOLTAGE) {
            helper.fail("input voltage was " + before.inputVoltage() + ", expected " + EXPECTED_INPUT_VOLTAGE);
            return;
        }
        // test_machine 无配方逻辑：状态须精确为 UNKNOWN、进度须为 0（而非仅「非空 / 非负」）。
        if (!GtMachineSnapshot.STATUS_UNKNOWN.equals(before.status())) {
            helper.fail("status was " + before.status() + ", expected " + GtMachineSnapshot.STATUS_UNKNOWN
                    + " for a machine without recipe logic");
            return;
        }
        if (before.progress() != 0 || before.maxProgress() != 0) {
            helper.fail("recipe progress must be 0 without recipe logic: progress=" + before.progress()
                    + " maxProgress=" + before.maxProgress());
            return;
        }

        // 服务端权威注入：能量必须精确增加注入量（未注入时该断言失败——见 RED 证据）。
        long changed = GtMachineDebug.charge(helper.getLevel(), absolute, INJECT_EU);
        if (changed != INJECT_EU) {
            helper.fail("server-side charge changed " + changed + " EU, expected " + INJECT_EU);
            return;
        }

        GtMachineSnapshot after = GtMachineSnapshots.at(helper.getLevel(), absolute).orElseThrow();
        if (after.energyStored() != INJECT_EU) {
            helper.fail("authoritative energyStored was " + after.energyStored()
                    + ", expected exactly " + INJECT_EU + " after injection");
            return;
        }

        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void snapshotAbsentForNonMachineBlock(GameTestHelper helper) {
        BlockPos relative = new BlockPos(1, 1, 1);
        helper.setBlock(relative, Blocks.STONE.defaultBlockState());

        if (GtMachineSnapshots.at(helper.getLevel(), helper.absolutePos(relative)).isPresent()) {
            helper.fail("stone block must not yield a GT machine snapshot");
            return;
        }
        if (GtMachineSnapshots.isGtMachine(helper.getLevel(), helper.absolutePos(relative))) {
            helper.fail("stone block must not be reported as a GT machine");
            return;
        }
        if (GtMachineDebug.charge(helper.getLevel(), helper.absolutePos(relative), INJECT_EU) != 0L) {
            helper.fail("charging a non-machine block must be a no-op (return 0)");
            return;
        }
        helper.succeed();
    }
}
