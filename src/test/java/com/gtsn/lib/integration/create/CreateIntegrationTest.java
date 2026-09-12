package com.gtsn.lib.integration.create;

import com.gtsn.lib.api.IntegrationModule;
import com.gtsn.lib.api.IntegrationRegistry;
import com.gtsn.lib.integration.IntegrationTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link CreateIntegration} 骨架行为（#7）：注入假 presence，覆盖在场初始化与缺席不实例化两态。
 */
class CreateIntegrationTest {

    @Test
    void presentModuleIsInitialized() {
        IntegrationRegistry registry = IntegrationTestSupport.withOnlyPresent("create");

        assertEquals(1, registry.initializeAll(), "在场时应实例化并初始化机械动力模块");

        IntegrationModule module = registry.get("create").orElseThrow();
        assertEquals("create", module.modId());
        assertEquals("Create", module.displayName());
    }

    @Test
    void absentModuleIsNotInitialized() {
        IntegrationRegistry registry = IntegrationTestSupport.withAllAbsent();

        assertEquals(0, registry.initializeAll(), "缺席时不应实例化任何模块");
        assertTrue(registry.get("create").isEmpty(), "缺席时不应返回机械动力模块");
        assertTrue(registry.failures().isEmpty(), "缺席时不应触发任何类链接或初始化失败");
    }
}
