package com.gtsn.lib;

import com.gtsn.lib.api.IntegrationTargets;
import com.gtsn.lib.core.ForgeModPresence;
import com.gtsn.lib.core.GtsnBuildInfo;
import com.gtsn.lib.core.GtsnIntegrations;
import com.gtsn.lib.core.config.GtsnConfig;
import com.gtsn.lib.gt.adapter.GtContentRegistration;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * GTSNLib 入口。仅承载库自身的初始化；按 ADR-0003，本类不得出现任何可选联动 mod 的类型。
 */
@Mod(GTSNLib.MOD_ID)
public class GTSNLib {
    public static final String MOD_ID = "gtsnlib";
    private static final Logger LOGGER = LogUtils.getLogger();

    public GTSNLib() {
        LOGGER.info("[GTSNLib] Loading GTSNLib {}", GtsnBuildInfo.VERSION);
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        // 通用注册简化层（#15）：机器必须在 GTCEu 冻结 gtceu:machine 之前的 GenericEvent 窗口注册，
        // 该事件需经 addGenericListener 订阅；经适配层接线，入口不接触 GTCEu 类型（ADR-0005）。
        GtContentRegistration.subscribe(modEventBus);
        GtsnConfig.init();
        ForgeModPresence presence = new ForgeModPresence();
        GtsnIntegrations.bootstrap(presence);
        // Mekanism 化学注册（#14）：其化学注册表经 Forge RegisterEvent 填充，必须在注册事件前接线，
        // 故在构造期判定在场后调用。仅当 Mekanism 在场才引用隔离包类，缺席时该类不被链接（ADR-0003）。
        if (presence.isLoaded(IntegrationTargets.MEKANISM.modId())) {
            com.gtsn.lib.integration.mekanism.MekanismChemicalRegistration.subscribe(modEventBus);
        }
    }
}
