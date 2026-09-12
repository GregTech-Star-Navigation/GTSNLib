package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.GTSNLib;
import com.mojang.logging.LogUtils;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus;
import org.slf4j.Logger;

/**
 * 启动探针：在服务端完成加载后，经 {@link GtAdapter} 查询已知 GT 材料并写入日志，
 * 作为适配层在真实运行环境中的运行时证据。
 *
 * <p>本类只依赖适配层门面，不引用任何 GTCEu 类型；加载时机选在服务端启动完成，
 * 以确保 GTCEu 材料注册表已就绪。</p>
 */
@Mod.EventBusSubscriber(modid = GTSNLib.MOD_ID, bus = Bus.FORGE)
public final class GtStartupProbe {

    private static final Logger LOGGER = LogUtils.getLogger();

    private GtStartupProbe() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        GtAdapter adapter = GtAdapter.get();
        if (!adapter.available()) {
            LOGGER.warn("[GTSNLib] GT adapter probe: material registry unavailable");
            return;
        }
        LOGGER.info("[GTSNLib] {}", GtAdapterReport.detectedLogLine(adapter.query(GtAdapter.DEFAULT_PROBE_MATERIAL)));
    }
}
