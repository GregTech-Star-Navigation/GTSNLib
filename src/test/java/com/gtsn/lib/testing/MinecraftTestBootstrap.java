package com.gtsn.lib.testing;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;

import java.lang.reflect.Field;

/**
 * 无游戏环境单测的 Minecraft 注册表引导（headless）。
 *
 * <p>{@code ItemStack} 等原版类的静态初始化会向 {@link BuiltInRegistries} 注册编解码器，而
 * {@link BuiltInRegistries} 的构造要求 {@link Bootstrap} 已标记为 bootstrapped。Forge 给
 * {@link Bootstrap#bootStrap()} 打了补丁，会额外初始化网络层（需要事件总线 / mod 加载环境），
 * 在纯 JUnit 环境下不可用。</p>
 *
 * <p>因此这里只做最小引导：先经反射置位 {@code isBootstrapped}，再直接调用
 * {@link BuiltInRegistries#bootStrap()}，只装载原版注册表内容，不触碰 Forge 网络栈。</p>
 */
public final class MinecraftTestBootstrap {

    private static boolean done;

    private MinecraftTestBootstrap() {
    }

    /** 幂等引导；多次调用只执行一次。 */
    public static synchronized void ensure() {
        if (done) {
            return;
        }
        try {
            SharedConstants.tryDetectVersion();
            Field flag = Bootstrap.class.getDeclaredField("isBootstrapped");
            flag.setAccessible(true);
            flag.setBoolean(null, true);
        } catch (ReflectiveOperationException failure) {
            throw new IllegalStateException("failed to set Minecraft bootstrap flag", failure);
        }
        BuiltInRegistries.bootStrap();
        done = true;
    }
}
