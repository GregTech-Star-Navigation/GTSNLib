package com.gtsn.lib.ui.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtsn.lib.testing.TtfCmap;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 随库打包的字体资源（#23）：字体定义 JSON（{@code ttf} + {@code reference minecraft:default}）、
 * 子集 TTF 的 cmap 覆盖、许可文本，以及缺字形时按 provider 顺序回退的选择逻辑（headless）。
 *
 * <p>回退语义与 {@code FontSet} 一致：providers 按声明顺序展平，**首个命中者胜**；
 * 自定义字体不会自动回退原版，必须显式声明 {@code reference} 层。</p>
 */
class FontResourceFilesTest {

    private static final String FONT_DIR = "assets/gtsnlib/font/";
    private static final String FONT_NAME = "sarasa_ui_sc";
    private static final String FONT_JSON = FONT_DIR + FONT_NAME + ".json";
    private static final String FONT_TTF = FONT_DIR + FONT_NAME + ".ttf";
    private static final String FONT_LICENSE = FONT_DIR + "LICENSE-Sarasa-Gothic.txt";
    private static final String THEME_JSON = "assets/gtsnlib/ui/themes/default.json";

    private static byte[] readBytes(String classpathResource) throws IOException {
        try (InputStream stream = FontResourceFilesTest.class.getClassLoader()
                .getResourceAsStream(classpathResource)) {
            assertNotNull(stream, "classpath 资源缺失: " + classpathResource);
            return stream.readAllBytes();
        }
    }

    private static JsonObject readJson(String classpathResource) throws IOException {
        try (InputStream stream = FontResourceFilesTest.class.getClassLoader()
                .getResourceAsStream(classpathResource)) {
            assertNotNull(stream, "classpath 资源缺失: " + classpathResource);
            return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        }
    }

    private static JsonArray providers() throws IOException {
        return readJson(FONT_JSON).getAsJsonArray("providers");
    }

    @Test
    void shippedFontDefinitionChainsTtfThenVanillaReference() throws IOException {
        JsonArray providers = providers();
        assertEquals(2, providers.size(), "providers 顺序：自定义 TTF 在前、原版回退层在后");

        JsonObject ttf = providers.get(0).getAsJsonObject();
        assertEquals("ttf", ttf.get("type").getAsString());
        assertEquals("gtsnlib:font/" + FONT_NAME + ".ttf", ttf.get("file").getAsString());
        assertTrue(ttf.get("size").getAsFloat() > 0, "ttf provider 必须给出字号");
        assertTrue(ttf.get("oversample").getAsFloat() >= 1.0F, "ttf provider 采样倍率");
        assertTrue(ttf.getAsJsonArray("skip").toString().contains(" "), "跳过空格，交由原版空格度量");

        JsonObject reference = providers.get(1).getAsJsonObject();
        assertEquals("reference", reference.get("type").getAsString());
        assertEquals("minecraft:default", reference.get("id").getAsString(),
                "缺字形回退到原版字库的关键声明");
    }

    @Test
    void bundledTtfIsPresentAndParsesAsSfnt() throws IOException {
        byte[] bytes = readBytes(FONT_TTF);
        assertTrue(bytes.length > 100_000, "子集字体应非空且具备实际字形: " + bytes.length + " bytes");
        String version = new String(bytes, 0, 4, StandardCharsets.ISO_8859_1);
        assertTrue(version.equals("\u0000\u0001\u0000\u0000") || version.equals("true") || version.equals("OTTO"),
                "sfnt version: " + version);

        TtfCmap cmap = TtfCmap.parse(bytes);
        assertTrue(cmap.covers('A'), "ASCII 基础");
        assertTrue(cmap.covers('中'), "常用汉字");
        assertTrue(cmap.covers('文'), "常用汉字");
        assertTrue(cmap.covers('，'), "全角标点");
        assertTrue(cmap.covers('。'), "CJK 句号");
        assertTrue(cmap.covers('、'), "CJK 顿号");
        assertTrue(cmap.covers('→'), "箭头符号");
        assertTrue(cmap.covers('×'), "乘号");
        assertTrue(cmap.covers('①'), "带圈数字");
        assertTrue(cmap.covers('≤'), "数学比较符");
    }

    @Test
    void subsetOmitsGb2312Level2SoReferenceLayerIsRequired() throws IOException {
        TtfCmap cmap = TtfCmap.parse(readBytes(FONT_TTF));
        assertFalse(cmap.covers('龘'), "二级字库字符不在子集内（从而需要原版回退层）");
        assertFalse(cmap.covers('\uE000'), "私用区字符不在子集内");
        assertEquals("minecraft:default", resolve('龘'), "缺字形按 provider 顺序落到 reference 层");
    }

    @Test
    void providerOrderSelectsFirstMatchAndSkipsSpaces() throws IOException {
        JsonArray providers = providers();
        TtfCmap subset = TtfCmap.parse(readBytes(FONT_TTF));

        assertEquals("gtsnlib:font/" + FONT_NAME + ".ttf", resolve(providers, subset, '中'), "子集内字形由 TTF 服务");
        assertEquals("gtsnlib:font/" + FONT_NAME + ".ttf", resolve(providers, subset, 'A'), "ASCII 由 TTF 服务");
        assertEquals("minecraft:default", resolve(providers, subset, ' '), "space 被 ttf.skip 跳过 → 原版空格");
        assertEquals("minecraft:default", resolve(providers, subset, '龘'), "子集未覆盖 → 原版回退层");
    }

    @Test
    void bundledFontLicensesUnderOfl() throws IOException {
        String license = new String(readBytes(FONT_LICENSE), StandardCharsets.UTF_8);
        assertFalse(license.isBlank(), "许可文本随库打包");
        assertTrue(license.contains("SIL Open Font License"), "OFL 声明");
        assertTrue(license.contains("1.1"), "OFL 版本");
        assertTrue(license.contains("Preamble"), "完整许可正文而非仅版权行");
        assertTrue(license.toLowerCase(java.util.Locale.ROOT).contains("permission & conditions"),
                "完整许可正文而非仅版权行");
    }

    @Test
    void defaultThemePointsAtBundledFont() throws IOException {
        try (InputStream stream = FontResourceFilesTest.class.getClassLoader()
                .getResourceAsStream(THEME_JSON)) {
            assertNotNull(stream, "classpath 资源缺失: " + THEME_JSON);
            ThemeDefinition definition = ThemeParser.parse(
                    ThemeRegistry.DEFAULT_ID,
                    new InputStreamReader(stream, StandardCharsets.UTF_8));
            FontId font = definition.textFont().orElseThrow(
                    () -> new AssertionError("默认主题未声明 text.font"));
            assertEquals(FontId.parse("gtsnlib:" + FONT_NAME), font);
            assertNotNull(FontResourceFilesTest.class.getClassLoader()
                            .getResourceAsStream("assets/" + font.namespace() + "/font/" + font.path() + ".json"),
                    "主题字体 id 必须指向随库打包的字体定义");
        }
    }

    /** 按 {@code FontSet} 展平语义解析某码位由哪一层服务（headless 模拟）。 */
    private static String resolve(int codePoint) throws IOException {
        return resolve(providers(), TtfCmap.parse(readBytes(FONT_TTF)), codePoint);
    }

    private static String resolve(JsonArray providers, TtfCmap subset, int codePoint) {
        for (int i = 0; i < providers.size(); i++) {
            JsonObject provider = providers.get(i).getAsJsonObject();
            switch (provider.get("type").getAsString()) {
                case "ttf" -> {
                    boolean skipped = false;
                    JsonArray skip = provider.getAsJsonArray("skip");
                    if (skip != null) {
                        for (int s = 0; s < skip.size(); s++) {
                            String skippedChars = skip.get(s).getAsString();
                            for (int c = 0; c < skippedChars.length(); c++) {
                                if (skippedChars.charAt(c) == (char) codePoint) {
                                    skipped = true;
                                }
                            }
                        }
                    }
                    if (!skipped && subset.covers(codePoint)) {
                        return provider.get("file").getAsString();
                    }
                }
                case "reference" -> {
                    // 原版 minecraft:default：1.20.1 随包 unihex 覆盖 BMP 全平面。
                    if (codePoint <= 0xFFFF) {
                        return provider.get("id").getAsString();
                    }
                }
                default -> {
                    // 其它 provider（bitmap/space/unihex）不在本测试的字体链中。
                }
            }
        }
        return "missing";
    }
}
