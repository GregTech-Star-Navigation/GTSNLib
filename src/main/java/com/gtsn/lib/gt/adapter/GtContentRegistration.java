package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.gt.registration.BlockRegistration;
import com.gtsn.lib.gt.registration.BlockSpec;
import com.gtsn.lib.gt.registration.ItemRegistration;
import com.gtsn.lib.gt.registration.ItemSpec;
import com.gtsn.lib.gt.registration.MachineRegistration;
import com.gtsn.lib.gt.registration.MachineSpec;

import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 通用注册简化层（#15）的游戏内接线：在 GTCEu 的 addon 材料注册时机（{@link MaterialEvent}），
 * 通过声明式 helper 注册一个演示方块（含方块物品）、一个演示物品与一台最小机器，作为游戏内证据。
 *
 * <p>时机说明：注册必须发生在 Minecraft 的 {@code RegisterEvent} 之前，而 GTCEu {@code CommonProxy#init}
 * 会在 {@code MaterialEvent} 之后为各命名空间材料注册表的 registrate 挂接 {@code RegisterEvent} 监听，
 * 因此在此声明条目会被真正注册。本类位于适配层，允许直接引用 GTCEu 事件类型（ADR-0005）。</p>
 */
@Mod.EventBusSubscriber(modid = GTSNLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GtContentRegistration {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAMESPACE = GTSNLib.MOD_ID;

    private GtContentRegistration() {
    }

    /** 经通用注册 helper 注册演示方块 / 物品 / 机器，验证 GTRegistrate 翻译链。 */
    @SubscribeEvent
    public static void onMaterial(MaterialEvent event) {
        GtAdapter adapter = GtAdapter.get();

        BlockRegistration block = adapter.registerBlock(BlockSpec.builder(NAMESPACE, GtAdapter.DEMO_BLOCK_ID)
                .strength(2.0F, 3.0F)
                .withItem(true)
                .displayName("Test Block")
                .build());
        ItemRegistration item = adapter.registerItem(ItemSpec.builder(NAMESPACE, GtAdapter.DEMO_ITEM_ID)
                .maxStackSize(16)
                .displayName("Test Item")
                .build());
        MachineRegistration machine = adapter.registerMachine(MachineSpec.builder(NAMESPACE, GtAdapter.DEMO_MACHINE_ID)
                .tier(1)
                .displayName("Test Machine")
                .build());

        LOGGER.info("[GTSNLib] registered demo content via registration helper: "
                        + "block {} (item {}), item {}, machine {}",
                block.resourceLocation(), block.itemResourceLocation(),
                item.resourceLocation(), machine.resourceLocation());
    }
}
