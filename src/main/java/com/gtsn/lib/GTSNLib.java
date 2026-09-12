package com.gtsn.lib;

import com.gtsn.lib.core.ForgeModPresence;
import com.gtsn.lib.core.GtsnBuildInfo;
import com.gtsn.lib.core.GtsnIntegrations;
import com.gtsn.lib.core.config.GtsnConfig;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.common.Mod;
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
        GtsnConfig.init();
        GtsnIntegrations.bootstrap(new ForgeModPresence());
    }
}
