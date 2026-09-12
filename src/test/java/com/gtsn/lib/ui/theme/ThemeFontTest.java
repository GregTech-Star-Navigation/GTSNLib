package com.gtsn.lib.ui.theme;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 字体资源标识与主题字体接线（#23，无 MC）：
 *
 * <ul>
 *   <li>{@link FontId} 的解析 / 规范形与 {@link FontId#VANILLA} 哨兵；</li>
 *   <li>主题 JSON {@code text.font} 的解析与非法值拒绝；</li>
 *   <li>沿 {@code extends} 链的字体继承与缺省回退（未声明 = 原版默认字体）；</li>
 *   <li>{@link ThemeTextStyle} 的向后兼容构造（两参数 = 原版字体）。</li>
 * </ul>
 */
class ThemeFontTest {

    private static final ThemeId DEFAULT = ThemeRegistry.DEFAULT_ID;
    private static final ThemeId LIGHT = ThemeId.of("gtsnlib", "light");
    private static final FontId SARASA = FontId.of("gtsnlib", "sarasa_ui_sc");

    @Test
    void fontIdParsesNamespaceAndDefaultsToLibraryNamespace() {
        assertEquals(new FontId("gtsnlib", "sarasa_ui_sc"), FontId.of("gtsnlib", "sarasa_ui_sc"));

        FontId defaulted = FontId.parse("sarasa_ui_sc");
        assertEquals("gtsnlib", defaulted.namespace(), "缺省命名空间为本库");
        assertEquals("sarasa_ui_sc", defaulted.path());

        FontId explicit = FontId.parse("minecraft:default");
        assertEquals("minecraft", explicit.namespace());
        assertEquals("default", explicit.path());
        assertEquals("minecraft:default", explicit.location());
    }

    @Test
    void vanillaSentinelIsMinecraftDefaultFont() {
        assertEquals("minecraft", FontId.VANILLA.namespace());
        assertEquals("default", FontId.VANILLA.path());
    }

    @Test
    void invalidFontIdIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> FontId.parse(""));
        assertThrows(IllegalArgumentException.class, () -> FontId.parse("BAD!"));
        assertThrows(IllegalArgumentException.class, () -> FontId.of("gtsnlib", "bad path"));
    }

    @Test
    void parserReadsTextFont() {
        ThemeDefinition def = ThemeParser.parse(LIGHT,
                "{\"text\":{\"shadow\":false,\"line_spacing\":1,\"font\":\"gtsnlib:sarasa_ui_sc\"}}");
        assertEquals(SARASA, def.textFont().orElseThrow());
    }

    @Test
    void parserKeepsFontAbsentWhenNotDeclared() {
        ThemeDefinition def = ThemeParser.parse(LIGHT, "{\"text\":{\"shadow\":true}}");
        assertTrue(def.textFont().isEmpty());
    }

    @Test
    void parserRejectsInvalidFont() {
        assertThrows(ThemeParseException.class,
                () -> ThemeParser.parse(LIGHT, "{\"text\":{\"font\":\"BAD!\"}}"));
        assertThrows(ThemeParseException.class,
                () -> ThemeParser.parse(LIGHT, "{\"text\":{\"font\":12}}"));
    }

    @Test
    void themeTextStyleWithoutFontDefaultsToVanilla() {
        ThemeTextStyle legacy = new ThemeTextStyle(true, 2);
        assertTrue(legacy.shadow());
        assertEquals(2, legacy.lineSpacing());
        assertEquals(FontId.VANILLA, legacy.fontId(), "两参数构造保持既有语义：原版默认字体");
        assertEquals(FontId.VANILLA, ThemeTextStyle.DEFAULT.fontId());
    }

    @Test
    void themeInheritsAndOverridesFontAlongExtendsChain() {
        ThemeDefinition parent = ThemeDefinition.builder(DEFAULT)
                .textFont(SARASA)
                .build();
        ThemeDefinition child = ThemeDefinition.builder(LIGHT)
                .parent(DEFAULT)
                .build();

        Theme light = ThemeResolver.resolve(List.of(parent, child)).get(LIGHT);
        assertEquals(SARASA, light.textStyle().fontId(), "未覆盖主题继承父主题字体");

        ThemeDefinition override = ThemeDefinition.builder(DEFAULT)
                .textFont(SARASA)
                .build();
        ThemeDefinition grandChild = ThemeDefinition.builder(ThemeId.of("gtsnlib", "amber"))
                .parent(DEFAULT)
                .textFont(FontId.of("gtsnlib", "other_font"))
                .build();
        Theme amber = ThemeResolver.resolve(List.of(override, grandChild)).get(ThemeId.of("gtsnlib", "amber"));
        assertEquals(FontId.of("gtsnlib", "other_font"), amber.textStyle().fontId(), "子主题显式覆盖字体");
    }

    @Test
    void themeWithoutDeclaredFontResolvesToVanilla() {
        ThemeDefinition definition = ThemeDefinition.builder(LIGHT).build();
        Theme theme = ThemeResolver.resolve(List.of(definition)).get(LIGHT);
        assertEquals(FontId.VANILLA, theme.textStyle().fontId(), "链上均未声明字体时回退原版默认");
    }

    @Test
    void definitionBuilderCopyPreservesFont() {
        ThemeDefinition original = ThemeDefinition.builder(LIGHT).textFont(SARASA).build();
        ThemeDefinition copy = original.toBuilder().build();
        assertEquals(SARASA, copy.textFont().orElseThrow());
    }
}
