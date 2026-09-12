package com.gtsn.lib;

import com.gtsn.lib.core.GtsnBuildInfo;
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
        // T1: 联动数为 0（框架尚未注册任何联动模块）。
        LOGGER.info("[GTSNLib] {}", GtsnBuildInfo.formatStatus(GtsnBuildInfo.VERSION, 0));
    }
}
