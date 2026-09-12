package com.gtsn.lib.ui.theme;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全局主题上下文：默认值、切换 / 回退、确定性轮换、注册表替换与重置。
 */
class ThemeContextTest {

    private static final ThemeId DEFAULT = ThemeRegistry.DEFAULT_ID;
    private static final ThemeId LIGHT = ThemeId.of("gtsnlib", "light");
    private static final ThemeId AMBER = ThemeId.of("gtsnlib", "amber");

    private static ThemeDefinition definition(String path) {
        return ThemeDefinition.builder(ThemeId.of("gtsnlib", path)).build();
    }

    @BeforeEach
    @AfterEach
    void resetContext() {
        ThemeContext.reset();
    }

    @Test
    void activeDefaultsToBuiltinDefault() {
        assertEquals(DEFAULT, ThemeContext.activeId());
        assertEquals(DEFAULT, ThemeContext.active().id());
        assertEquals(0xFFE6E6E6, ThemeContext.active().color(ThemeColorRole.TEXT));
        assertEquals(1, ThemeContext.registry().size());
    }

    @Test
    void setActiveSwitchesThemeAndUnknownFallsBack() {
        ThemeContext.setRegistry(ThemeResolver.resolve(List.of(
                ThemeDefinition.builder(DEFAULT).color(ThemeColorRole.TEXT, 0xFF010101).build(),
                ThemeDefinition.builder(LIGHT).parent(DEFAULT).color(ThemeColorRole.TEXT, 0xFF020202).build())));

        assertTrue(ThemeContext.setActive(LIGHT));
        assertEquals(LIGHT, ThemeContext.activeId());
        assertEquals(0xFF020202, ThemeContext.active().color(ThemeColorRole.TEXT));

        assertFalse(ThemeContext.setActive(ThemeId.of("gtsnlib", "missing")), "未知主题拒绝切换");
        assertEquals(DEFAULT, ThemeContext.activeId(), "未知主题回退默认");
    }

    @Test
    void cycleRotatesInRegistryOrderAndWraps() {
        ThemeContext.setRegistry(ThemeResolver.resolve(List.of(
                definition("default"), definition("amber"), definition("light"))));

        assertEquals(AMBER, ThemeContext.cycle(), "默认之后的第一个主题");
        assertEquals(AMBER, ThemeContext.activeId());
        assertEquals(LIGHT, ThemeContext.cycle());
        assertEquals(DEFAULT, ThemeContext.cycle(), "轮换回环");
        assertEquals(DEFAULT, ThemeContext.activeId());
    }

    @Test
    void setRegistryKeepsActiveThemeWhenStillPresent() {
        ThemeRegistry withLight = ThemeResolver.resolve(List.of(definition("light")));
        ThemeContext.setRegistry(withLight);
        assertTrue(ThemeContext.setActive(LIGHT));

        ThemeRegistry stillWithLight = ThemeResolver.resolve(List.of(definition("light"), definition("amber")));
        ThemeContext.setRegistry(stillWithLight);
        assertEquals(LIGHT, ThemeContext.activeId(), "新注册表仍含激活主题则保留");

        ThemeRegistry withoutLight = ThemeResolver.resolve(List.of(definition("amber")));
        ThemeContext.setRegistry(withoutLight);
        assertEquals(DEFAULT, ThemeContext.activeId(), "激活主题消失则回退默认");
    }

    @Test
    void resetRestoresBuiltinRegistry() {
        ThemeContext.setRegistry(ThemeResolver.resolve(List.of(definition("light"))));
        ThemeContext.setActive(LIGHT);

        ThemeContext.reset();

        assertEquals(1, ThemeContext.registry().size());
        assertEquals(DEFAULT, ThemeContext.activeId());
        assertEquals(DEFAULT, ThemeContext.active().id());
    }
}
