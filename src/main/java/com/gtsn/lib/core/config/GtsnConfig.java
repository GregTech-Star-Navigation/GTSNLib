package com.gtsn.lib.core.config;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * 配置框架注册入口：按物理侧注册 common / client / server 三份配置。
 *
 * <p>common 恒注册；client 仅在客户端注册；server 仅在专职服务端注册（集成服务端由客户端配置覆盖）。
 * 在 {@code @Mod} 构造期调用（与 GTCEu 的 ConfigHolder 一致）。</p>
 */
public final class GtsnConfig {

    private GtsnConfig() {
    }

    public static void init() {
        GtsnCommonConfig.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            GtsnClientConfig.init();
        }
        if (FMLEnvironment.dist.isDedicatedServer()) {
            GtsnServerConfig.init();
        }
    }
}
