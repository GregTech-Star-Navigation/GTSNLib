package com.gtsn.lib.ui.theme;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 随库分发的主题资源（{@code assets/gtsnlib/ui/themes/*.json}）与纹理资源：
 * 经类路径资源解析（与客户端 ResourceManager 同一批文件），验证真实资源可解析、可继承、可回退。
 */
class ThemeResourceFilesTest {

    private static final String THEME_DIR = "assets/gtsnlib/ui/themes/";
    private static final String TEXTURE_DIR = "assets/gtsnlib/textures/gui/";

    private static final ThemeId DEFAULT = ThemeRegistry.DEFAULT_ID;
    private static final ThemeId LIGHT = ThemeId.of("gtsnlib", "light");
    private static final ThemeId AMBER = ThemeId.of("gtsnlib", "amber");

    private static ThemeDefinition load(String fileName) throws IOException {
        String location = THEME_DIR + fileName;
        try (InputStream stream = ThemeResourceFilesTest.class.getClassLoader().getResourceAsStream(location)) {
            assertNotNull(stream, "classpath 资源缺失: " + location);
            ThemeId id = ThemeId.parse("gtsnlib:" + fileName.substring(0, fileName.length() - ".json".length()));
            return ThemeParser.parse(id, new InputStreamReader(stream, StandardCharsets.UTF_8));
        }
    }

    @Test
    void shippedThemesParseAndResolve() throws IOException {
        ThemeRegistry registry = ThemeResolver.resolve(List.of(
                load("amber.json"), load("default.json"), load("light.json")));

        assertEquals(List.of(DEFAULT, AMBER, LIGHT), registry.ids());

        Theme light = registry.get(LIGHT);
        assertEquals(0xFFF4F6FA, light.color(ThemeColorRole.PANEL_BACKGROUND));
        assertEquals(0xFF2A3038, light.color(ThemeColorRole.TEXT));
        assertEquals(DEFAULT, light.parent().orElseThrow().id());

        Theme amber = registry.get(AMBER);
        assertEquals(0xFF201A10, amber.color(ThemeColorRole.PANEL_BACKGROUND));
        assertEquals(ThemeColorRole.SLOT_EDGE_LIGHT.defaultArgb(),
                amber.color(ThemeColorRole.SLOT_EDGE_LIGHT), "未覆盖角色继承默认主题（即枚举默认值）");
        assertEquals(10, amber.spacing(Spacing.LG), "琥珀主题显式间距");
        assertEquals(12, amber.spacing(Spacing.XL), "琥珀主题显式间距");
        assertEquals(6, amber.spacing(Spacing.MD), "未覆盖间距继承默认主题");
    }

    @Test
    void defaultThemeDeclaresEveryColorRole() throws IOException {
        ThemeDefinition definition = load("default.json");
        for (ThemeColorRole role : ThemeColorRole.values()) {
            assertTrue(definition.color(role).isPresent(), "默认主题缺角色: " + role.key());
        }
    }

    @Test
    void lightThemeReferencesShippedPanelTexture() throws IOException {
        ThemeDefinition definition = load("light.json");
        ThemeId texture = definition.texture(ThemeTextureRole.PANEL).orElseThrow();
        assertEquals(ThemeId.of("gtsnlib", "textures/gui/panel_light.png"), texture);

        try (InputStream stream = ThemeResourceFilesTest.class.getClassLoader()
                .getResourceAsStream(TEXTURE_DIR + "panel_light.png")) {
            assertNotNull(stream, "classpath 纹理资源缺失: " + TEXTURE_DIR + "panel_light.png");
            assertTrue(stream.read() >= 0, "纹理资源非空");
        }
    }

    @Test
    void noShippedThemeDeclaresUnknownColorRole() throws IOException {
        for (String fileName : List.of("default.json", "light.json", "amber.json")) {
            ThemeDefinition definition = load(fileName);
            assertEquals(definition.colors().size(),
                    definition.colors().keySet().stream().distinct().count(),
                    fileName + " 颜色角色重复");
        }
    }
}
