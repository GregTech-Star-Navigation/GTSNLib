package com.gtsn.lib.core;

import com.gtsn.lib.core.config.GtsnCommonConfig;

import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * 演示内容门控的运行时解析：把 {@link DemoContentGate} 的纯谓词接到
 * {@code FMLEnvironment.production} 与 common 配置 {@code registerDemoContent} 上。
 *
 * <p>与纯谓词分离，使 {@link DemoContentGate} 保持无 Forge / Minecraft 依赖、可无头单测。</p>
 */
public final class DemoContentRuntime {

    private DemoContentRuntime() {
    }

    /** 当前是否登记 GTSNLib 演示内容（开发态恒是；生产态取决于配置）。 */
    public static boolean enabled() {
        return DemoContentGate.shouldRegister(FMLEnvironment.production, GtsnCommonConfig.demoContentOptedIn());
    }
}
