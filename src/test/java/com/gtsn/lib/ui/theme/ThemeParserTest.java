package com.gtsn.lib.ui.theme;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主题 JSON 解析：结构校验、颜色格式、可选小节缺省与非法输入的显式拒绝。
 *
 * <p>解析器不依赖 Minecraft，可在无客户端环境中测试。</p>
 */
class ThemeParserTest {

    private static final ThemeId LIGHT = ThemeId.of("gtsnlib", "light");

    private static final String FULL_JSON = """
            {
              "name": "浅色",
              "extends": "gtsnlib:default",
              "colors": {
                "background": "#E8ECF4F8",
                "panel_background": "#FFF4F6FA",
                "text": "#FF2A3038",
                "accent": "#FF2F6FB8"
              },
              "text": { "shadow": false, "line_spacing": 1 },
              "spacing": { "xs": 2, "sm": 4, "md": 6, "lg": 10, "xl": 14 },
              "rounding": { "small": 2, "medium": 4, "large": 8 },
              "textures": { "panel": "gtsnlib:textures/gui/panel_light.png" }
            }
            """;

    @Test
    void parsesFullThemeDefinition() {
        ThemeDefinition def = ThemeParser.parse(LIGHT, FULL_JSON);

        assertEquals(LIGHT, def.id());
        assertEquals("浅色", def.name());
        assertEquals(ThemeId.of("gtsnlib", "default"), def.parentId().orElseThrow());
        assertEquals(0xE8ECF4F8, def.color(ThemeColorRole.BACKGROUND).orElseThrow());
        assertEquals(0xFFF4F6FA, def.color(ThemeColorRole.PANEL_BACKGROUND).orElseThrow());
        assertEquals(0xFF2A3038, def.color(ThemeColorRole.TEXT).orElseThrow());
        assertEquals(0xFF2F6FB8, def.color(ThemeColorRole.ACCENT).orElseThrow());
        assertEquals(Boolean.FALSE, def.textShadow().orElseThrow());
        assertEquals(1, def.textLineSpacing().orElseThrow());
        assertEquals(10, def.spacing(Spacing.LG).orElseThrow());
        assertEquals(14, def.spacing(Spacing.XL).orElseThrow());
        assertEquals(8, def.rounding(RoundingSize.LARGE).orElseThrow());
        assertEquals(ThemeId.of("gtsnlib", "textures/gui/panel_light.png"),
                def.texture(ThemeTextureRole.PANEL).orElseThrow());
    }

    @Test
    void parsesFromReaderToo() {
        ThemeDefinition def = ThemeParser.parse(LIGHT, new StringReader(FULL_JSON));
        assertEquals("浅色", def.name());
    }

    @Test
    void sixDigitHexIsOpaque() {
        ThemeDefinition def = ThemeParser.parse(LIGHT, "{\"colors\":{\"text\":\"#123456\"}}");
        assertEquals(0xFF123456, def.color(ThemeColorRole.TEXT).orElseThrow());
    }

    @Test
    void hexWithoutHashIsAccepted() {
        ThemeDefinition def = ThemeParser.parse(LIGHT, "{\"colors\":{\"text\":\"FF123456\"}}");
        assertEquals(0xFF123456, def.color(ThemeColorRole.TEXT).orElseThrow());
    }

    @Test
    void missingSectionsStayEmpty() {
        ThemeDefinition def = ThemeParser.parse(LIGHT, "{}");

        assertTrue(def.colors().isEmpty());
        assertTrue(def.parentId().isEmpty());
        assertTrue(def.textShadow().isEmpty());
        assertTrue(def.textLineSpacing().isEmpty());
        assertTrue(def.texture(ThemeTextureRole.PANEL).isEmpty());
        assertEquals("light", def.name(), "未提供 name 时回退为主题 id 的路径段");
    }

    @Test
    void missingSpacingStepsStayEmpty() {
        ThemeDefinition def = ThemeParser.parse(LIGHT, "{\"spacing\":{\"md\":6}}");
        assertEquals(6, def.spacing(Spacing.MD).orElseThrow());
        assertTrue(def.spacing(Spacing.LG).isEmpty());
    }

    @Test
    void unknownColorRoleIsIgnored() {
        ThemeDefinition def = ThemeParser.parse(LIGHT, "{\"colors\":{\"not_a_role\":\"#FF000000\"}}");
        assertTrue(def.colors().isEmpty(), "未知颜色角色应被忽略（前向兼容）");
    }

    @Test
    void unknownTopLevelKeyIsIgnored() {
        ThemeDefinition def = ThemeParser.parse(LIGHT, "{\"future_section\":{\"x\":1}}");
        assertTrue(def.colors().isEmpty());
    }

    @Test
    void malformedJsonIsRejected() {
        assertThrows(ThemeParseException.class, () -> ThemeParser.parse(LIGHT, "not json"));
    }

    @Test
    void nonObjectRootIsRejected() {
        assertThrows(ThemeParseException.class, () -> ThemeParser.parse(LIGHT, "[1,2,3]"));
    }

    @Test
    void invalidColorIsRejected() {
        ThemeParseException error = assertThrows(ThemeParseException.class,
                () -> ThemeParser.parse(LIGHT, "{\"colors\":{\"text\":\"nope\"}}"));
        assertTrue(error.getMessage().contains("text"), error.getMessage());
    }

    @Test
    void wrongColorTypeIsRejected() {
        assertThrows(ThemeParseException.class, () -> ThemeParser.parse(LIGHT, "{\"colors\":{\"text\":12}}"));
    }

    @Test
    void invalidExtendsIsRejected() {
        assertThrows(ThemeParseException.class, () -> ThemeParser.parse(LIGHT, "{\"extends\":\"BAD!\"}"));
    }

    @Test
    void wrongNameTypeIsRejected() {
        assertThrows(ThemeParseException.class, () -> ThemeParser.parse(LIGHT, "{\"name\":42}"));
    }

    @Test
    void negativeSpacingIsRejected() {
        assertThrows(ThemeParseException.class, () -> ThemeParser.parse(LIGHT, "{\"spacing\":{\"md\":-1}}"));
    }

    @Test
    void invalidTextureLocationIsRejected() {
        assertThrows(ThemeParseException.class,
                () -> ThemeParser.parse(LIGHT, "{\"textures\":{\"panel\":\"BAD!\"}}"));
    }

    @Test
    void unknownTextureRoleIsIgnored() {
        ThemeDefinition def = ThemeParser.parse(LIGHT,
                "{\"textures\":{\"unknown_role\":\"gtsnlib:textures/gui/x.png\"}}");
        assertFalse(def.textures().containsKey(ThemeTextureRole.PANEL));
    }
}
