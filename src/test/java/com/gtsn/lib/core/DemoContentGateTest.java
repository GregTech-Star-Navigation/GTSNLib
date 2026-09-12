package com.gtsn.lib.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link DemoContentGate} 行为验证：演示内容登记门控谓词。
 *
 * <p>作为前置库，GTSNLib 不得默认把演示内容写进每一个安装的注册表/世界：生产环境默认关闭，
 * 仅当配置显式选择加入（{@code registerDemoContent=true}）时登记；开发环境（{@code production=false}，
 * 即 {@code runClient}/{@code runServer}/{@code runGameTestServer}/单测）恒为开启，以便既有 GameTest 保持绿色。</p>
 *
 * <p>纯布尔谓词，无 Forge / Minecraft 依赖，可无头单测。</p>
 */
class DemoContentGateTest {

    @Test
    void devEnvironmentAlwaysRegisters() {
        assertTrue(DemoContentGate.shouldRegister(false, false), "开发态（非生产）应恒登记演示内容");
        assertTrue(DemoContentGate.shouldRegister(false, true), "开发态（非生产）应恒登记演示内容");
    }

    @Test
    void productionSkipsByDefault() {
        assertFalse(DemoContentGate.shouldRegister(true, false), "生产态默认不得登记演示内容");
    }

    @Test
    void productionRegistersOnlyWhenOptedIn() {
        assertTrue(DemoContentGate.shouldRegister(true, true), "生产态显式选择加入后应登记演示内容");
    }
}
