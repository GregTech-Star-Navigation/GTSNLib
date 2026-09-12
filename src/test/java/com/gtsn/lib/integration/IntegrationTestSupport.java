package com.gtsn.lib.integration;

import com.gtsn.lib.api.IntegrationRegistry;

/**
 * 联动模块单测的共享装配：注入假 {@code ModPresence}，并走真实的
 * {@link IntegrationModules#registerAll} 登记路径装配模块工厂。
 *
 * <p>每个模块测试都在「注册表 + 双层 supplier」的真实链路上验证在场/缺席两态，
 * 不依赖任何 Forge 运行环境。</p>
 */
public final class IntegrationTestSupport {

    private IntegrationTestSupport() {
    }

    /** 仅 {@code presentModId} 在场，其余目标缺席。 */
    public static IntegrationRegistry withOnlyPresent(String presentModId) {
        IntegrationRegistry registry = new IntegrationRegistry(presentModId::equals);
        IntegrationModules.registerAll(registry);
        return registry;
    }

    /** 全部目标缺席。 */
    public static IntegrationRegistry withAllAbsent() {
        IntegrationRegistry registry = new IntegrationRegistry(modId -> false);
        IntegrationModules.registerAll(registry);
        return registry;
    }
}
