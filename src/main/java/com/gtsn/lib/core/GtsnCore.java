package com.gtsn.lib.core;

import com.gtsn.lib.GTSNLib;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/**
 * 库生命周期入口：在 FMLCommonSetup 阶段初始化联动模块并输出启动摘要。
 *
 * <p>按 ADR-0003，事件订阅类不得出现任何可选 mod 类型；此处只依赖纯库门面 {@link GtsnIntegrations}。</p>
 */
@Mod.EventBusSubscriber(modid = GTSNLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GtsnCore {

    private GtsnCore() {
    }

    @SubscribeEvent
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        GtsnIntegrations.initialize();
    }
}
