package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.theme.FontId;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeDefinition;
import com.gtsn.lib.ui.theme.ThemeId;
import com.gtsn.lib.ui.theme.ThemeResolver;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 主题字体度量接缝（#22 评审 F2-1）：度量必须绑定**生效主题字体**，不得逐次读全局
 * {@code ThemeContext.active()}——否则屏幕覆盖主题字体后，换行用全局字体、渲染用覆盖字体。
 *
 * <p>测试用一个「未绑定宽度（全局）」与「绑定宽度（主题字体）」不同的委托度量，证明
 * {@link ThemedTextMetrics} 用绑定字体度量 / 换行；若实现退回读全局，换行结果会不同，测试即失败。</p>
 */
class ThemedTextMetricsTest {

    private static final FontId THEME_FONT = FontId.of("test", "theme");
    private static final FontId OTHER_FONT = FontId.of("test", "other");

    /** 未指定字体时按 100px/字符（模拟全局字体），绑定 {@link #THEME_FONT} 时按 1px/字符。 */
    private static TextMetrics stub() {
        return new TextMetrics() {
            @Override
            public int width(String text) {
                return text.length() * 100;
            }

            @Override
            public int width(String text, FontId font) {
                return text.length() * (THEME_FONT.equals(font) ? 1 : 100);
            }

            @Override
            public int lineHeight() {
                return 9;
            }
        };
    }

    @Test
    void unqualifiedWidthUsesBoundThemeFontNotGlobal() {
        ThemedTextMetrics metrics = new ThemedTextMetrics(stub(), THEME_FONT);

        assertEquals(THEME_FONT, metrics.themeFont());
        assertEquals(4, metrics.width("xxxx"), "绑定字体度量，而非全局 100px/字符");
    }

    @Test
    void explicitFontOverrideWins() {
        ThemedTextMetrics metrics = new ThemedTextMetrics(stub(), THEME_FONT);

        assertEquals(100, metrics.width("x", OTHER_FONT), "逐控件显式字体优先");
    }

    @Test
    void wrappingFollowsBoundThemeFont() {
        // 14 字符；绑定字体 1px/字符 → 14px，可用宽度 20 → 单行。
        TextWidget bound = new TextWidget("aaaa bbbb cccc", new ThemedTextMetrics(stub(), THEME_FONT)).wrap(20);
        assertEquals(List.of("aaaa bbbb cccc"), bound.lines());

        // 同一文本若度量退回全局（100px/字符），会换行成多行——这正是本测试要抓到的错配。
        TextWidget unbound = new TextWidget("aaaa bbbb cccc", stub()).wrap(20);
        assertNotEquals(bound.lines(), unbound.lines());
        assertTrue(unbound.lines().size() > 1, "全局字体下应换行: " + unbound.lines());
    }

    @Test
    void themeFontFlowsThroughResolver() {
        Theme theme = ThemeResolver.resolve(List.of(
                        ThemeDefinition.builder(ThemeId.of("test", "themed")).textFont(THEME_FONT).build()))
                .get(ThemeId.of("test", "themed"));

        ThemedTextMetrics metrics = new ThemedTextMetrics(stub(), theme.textStyle().fontId());

        assertEquals(THEME_FONT, metrics.themeFont());
        assertEquals(5, metrics.width("xxxxx"));
    }
}
