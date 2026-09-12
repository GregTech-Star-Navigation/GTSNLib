package com.gtsn.lib.integration;

import com.gtsn.lib.api.IntegrationRegistry;
import com.gtsn.lib.api.IntegrationTargets;

/**
 * 联动模块登记入口：按 {@link IntegrationTargets} 声明顺序，经双层 supplier 登记全部目标模块。
 *
 * <p>按 ADR-0003，模块实现类名只出现在 {@code () -> () -> new XxxIntegration()} 的内层 supplier 中；
 * 外层 supplier 在构建期创建，内层 supplier 与模块实例只在目标 mod 确认在场时才产生，故本类不含任何
 * 目标 mod 类型，也不会在缺席时强制链接模块类。</p>
 */
public final class IntegrationModules {

    private IntegrationModules() {
    }

    /** 把所有联动模块工厂登记到给定注册表（同时登记对应目标 modId）。 */
    public static void registerAll(IntegrationRegistry registry) {
        registry.register(IntegrationTargets.MEKANISM.modId(),
                () -> () -> new com.gtsn.lib.integration.mekanism.MekanismIntegration());
        registry.register(IntegrationTargets.IMMERSIVE_ENGINEERING.modId(),
                () -> () -> new com.gtsn.lib.integration.immersiveengineering.ImmersiveEngineeringIntegration());
        registry.register(IntegrationTargets.CREATE.modId(),
                () -> () -> new com.gtsn.lib.integration.create.CreateIntegration());
    }
}
