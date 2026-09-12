package com.gtsn.lib.ui.theme;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主题注册表解析：{@code extends} 继承链、缺失角色回退、未知父主题 / 环的容错、
 * 默认主题保底与确定性排序。
 */
class ThemeResolverTest {

    private static final ThemeId DEFAULT = ThemeRegistry.DEFAULT_ID;
    private static final ThemeId LIGHT = ThemeId.of("gtsnlib", "light");
    private static final ThemeId AMBER = ThemeId.of("gtsnlib", "amber");

    private static ThemeDefinition definition(String path) {
        return ThemeDefinition.builder(ThemeId.of("gtsnlib", path)).build();
    }

    @Test
    void emptyDefinitionsYieldBuiltinDefaultOnly() {
        ThemeRegistry registry = ThemeResolver.resolve(List.of());

        assertEquals(1, registry.size());
        assertEquals(DEFAULT, registry.defaultTheme().id());
        assertEquals(0xFFE6E6E6, registry.defaultTheme().color(ThemeColorRole.TEXT));
        assertTrue(registry.find(DEFAULT).isPresent());
    }

    @Test
    void definitionsWithoutExplicitDefaultKeepBuiltinFallback() {
        ThemeRegistry registry = ThemeResolver.resolve(List.of(definition("light")));

        assertEquals(2, registry.size());
        assertEquals(DEFAULT, registry.defaultTheme().id());
        assertTrue(registry.find(LIGHT).isPresent());
    }

    @Test
    void childInheritsParentValuesThenFallsBackToRoleDefaults() {
        ThemeDefinition parent = ThemeDefinition.builder(DEFAULT)
                .color(ThemeColorRole.TEXT, 0xFF111111)
                .color(ThemeColorRole.PANEL_BACKGROUND, 0xFF222222)
                .build();
        ThemeDefinition child = ThemeDefinition.builder(LIGHT)
                .parent(DEFAULT)
                .color(ThemeColorRole.TEXT, 0xFF333333)
                .build();

        // 子定义先于父定义出现：解析必须支持前向引用。
        ThemeRegistry registry = ThemeResolver.resolve(List.of(child, parent));
        Theme light = registry.find(LIGHT).orElseThrow();

        assertEquals(0xFF333333, light.color(ThemeColorRole.TEXT), "子主题显式值优先");
        assertEquals(0xFF222222, light.color(ThemeColorRole.PANEL_BACKGROUND), "未覆盖角色继承父主题");
        assertEquals(ThemeColorRole.ACCENT.defaultArgb(), light.color(ThemeColorRole.ACCENT), "链上均未定义时回退角色默认值");
        assertEquals(DEFAULT, light.parent().orElseThrow().id());
    }

    @Test
    void unknownParentLeavesThemeResolvableWithoutParent() {
        ThemeDefinition child = ThemeDefinition.builder(LIGHT)
                .parent(ThemeId.of("gtsnlib", "missing"))
                .color(ThemeColorRole.TEXT, 0xFF444444)
                .build();

        ThemeRegistry registry = ThemeResolver.resolve(List.of(child));
        Theme light = registry.find(LIGHT).orElseThrow();

        assertTrue(light.parent().isEmpty(), "未知父主题不应产生父链");
        assertEquals(0xFF444444, light.color(ThemeColorRole.TEXT));
        assertEquals(ThemeColorRole.PANEL_BACKGROUND.defaultArgb(), light.color(ThemeColorRole.PANEL_BACKGROUND));
    }

    @Test
    void extendsCycleIsBrokenGracefully() {
        ThemeDefinition a = ThemeDefinition.builder(ThemeId.of("gtsnlib", "a")).parent(ThemeId.of("gtsnlib", "b"))
                .build();
        ThemeDefinition b = ThemeDefinition.builder(ThemeId.of("gtsnlib", "b")).parent(ThemeId.of("gtsnlib", "a"))
                .build();

        ThemeRegistry registry = ThemeResolver.resolve(List.of(a, b));

        assertTrue(registry.find(ThemeId.of("gtsnlib", "a")).isPresent());
        assertTrue(registry.find(ThemeId.of("gtsnlib", "b")).isPresent());
        assertEquals(ThemeColorRole.TEXT.defaultArgb(),
                registry.get(ThemeId.of("gtsnlib", "a")).color(ThemeColorRole.TEXT));
    }

    @Test
    void textSpacingRoundingAndTextureInherit() {
        ThemeId panelTexture = ThemeId.of("gtsnlib", "textures/gui/panel_light.png");
        ThemeId overrideTexture = ThemeId.of("gtsnlib", "textures/gui/panel_amber.png");
        ThemeDefinition parent = ThemeDefinition.builder(DEFAULT)
                .spacing(Spacing.SM, 3)
                .spacing(Spacing.LG, 12)
                .textShadow(true)
                .textLineSpacing(2)
                .rounding(RoundingSize.SMALL, 3)
                .texture(ThemeTextureRole.PANEL, panelTexture)
                .texture(ThemeTextureRole.TOOLTIP, panelTexture)
                .build();
        ThemeDefinition child = ThemeDefinition.builder(AMBER)
                .parent(DEFAULT)
                .spacing(Spacing.LG, 20)
                .texture(ThemeTextureRole.PANEL, overrideTexture)
                .build();

        Theme amber = ThemeResolver.resolve(List.of(parent, child)).get(AMBER);

        assertEquals(20, amber.spacing(Spacing.LG), "子主题覆盖间距");
        assertEquals(3, amber.spacing(Spacing.SM), "未覆盖间距继承父主题");
        assertTrue(amber.textStyle().shadow(), "文本度量继承");
        assertEquals(2, amber.textStyle().lineSpacing());
        assertEquals(3, amber.rounding(RoundingSize.SMALL), "圆角继承");
        assertEquals(overrideTexture, amber.texture(ThemeTextureRole.PANEL), "纹理覆盖");
        assertEquals(panelTexture, amber.texture(ThemeTextureRole.TOOLTIP), "未覆盖纹理角色继承");
        assertNull(amber.texture(ThemeTextureRole.SLOT), "主题链均未声明时纹理为空");
    }

    @Test
    void defaultThemeIsFirstThenIdsSortedAlphabetically() {
        ThemeRegistry registry = ThemeResolver.resolve(List.of(
                definition("light"), definition("amber"), definition("default")));

        assertEquals(List.of(DEFAULT, AMBER, LIGHT), registry.ids());
    }

    @Test
    void getFallsBackToDefaultForUnknownId() {
        ThemeRegistry registry = ThemeResolver.resolve(List.of(definition("light")));

        assertEquals(DEFAULT, registry.get(ThemeId.of("gtsnlib", "missing")).id());
        assertFalse(registry.find(ThemeId.of("gtsnlib", "missing")).isPresent());
    }
}
