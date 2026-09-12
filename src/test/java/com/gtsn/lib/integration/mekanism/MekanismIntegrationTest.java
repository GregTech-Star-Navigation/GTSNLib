package com.gtsn.lib.integration.mekanism;

import com.gtsn.lib.api.IntegrationModule;
import com.gtsn.lib.api.IntegrationRegistry;
import com.gtsn.lib.integration.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MekanismIntegration} 骨架行为（#5）：注入假 presence，覆盖在场初始化与缺席不实例化两态。
 */
class MekanismIntegrationTest {

    @Test
    void presentModuleIsInitialized() {
        IntegrationRegistry registry = IntegrationTestSupport.withOnlyPresent("mekanism");

        assertEquals(1, registry.initializeAll(), "在场时应实例化并初始化 Mekanism 模块");

        IntegrationModule module = registry.get("mekanism").orElseThrow();
        assertEquals("mekanism", module.modId());
        assertEquals("Mekanism", module.displayName());
    }

    @Test
    void absentModuleIsNotInitialized() {
        IntegrationRegistry registry = IntegrationTestSupport.withAllAbsent();

        assertEquals(0, registry.initializeAll(), "缺席时不应实例化任何模块");
        assertTrue(registry.get("mekanism").isEmpty(), "缺席时不应返回 Mekanism 模块");
        assertTrue(registry.failures().isEmpty(), "缺席时不应触发任何类链接或初始化失败");
    }
}
