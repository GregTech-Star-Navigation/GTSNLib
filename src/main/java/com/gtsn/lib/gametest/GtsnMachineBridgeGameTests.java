package com.gtsn.lib.gametest;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.api.GtMachineSnapshot;
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
 * 经 {@link GtMachineSnapshots} 取只读快照，并断言机器 id / tier / 能量与配方进度字段均存在。
 */
@GameTestHolder(GTSNLib.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GtsnMachineBridgeGameTests {

    private static final String MACHINE_ID = "gtsnlib:test_machine";

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
        GtMachineSnapshot snapshot = resolved.get();

        if (!MACHINE_ID.equals(snapshot.machineId())) {
            helper.fail("machine id was " + snapshot.machineId() + ", expected " + MACHINE_ID);
            return;
        }
        if (snapshot.tier() != 1) {
            helper.fail("machine tier was " + snapshot.tier() + ", expected 1 (test machine)");
            return;
        }
        if (snapshot.energyCapacity() <= 0L) {
            helper.fail("energy capacity was " + snapshot.energyCapacity()
                    + ", expected > 0 (test machine extends TieredEnergyMachine)");
            return;
        }
        if (snapshot.energyStored() < 0L || snapshot.inputVoltage() < 0L) {
            helper.fail("energy fields must be present and non-negative: stored=" + snapshot.energyStored()
                    + " inputVoltage=" + snapshot.inputVoltage());
            return;
        }
        if (snapshot.maxProgress() < 0 || snapshot.progress() < 0) {
            helper.fail("recipe progress fields must be present and non-negative: progress=" + snapshot.progress()
                    + " maxProgress=" + snapshot.maxProgress());
            return;
        }
        if (snapshot.status() == null || snapshot.status().isBlank()) {
            helper.fail("status text must be present");
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
        helper.succeed();
    }
}
