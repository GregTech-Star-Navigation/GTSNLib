package com.gtsn.lib.ui.client;

import com.gtsn.lib.ui.theme.FontId;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeContext;
import com.gtsn.lib.ui.widget.TextMetrics;
import com.gtsn.lib.ui.widget.ThemedTextMetrics;
import net.minecraft.client.gui.Font;

import java.util.Objects;

/**
 * 客户端文本度量（#23）：布局测量与渲染共用同一字体语义——绑定**生效主题**的文本字体
 * （{@link #ThemeFontMetrics(Font, Theme)} / {@link #ThemeFontMetrics(Font, FontId)}），
 * 指定字体时按 {@link ClientFonts} 应用（资源缺失回退原版）。
 *
 * <p>接缝修正（#22 评审 F2-1）：不再从 {@link ThemeContext#active()} 逐次读取全局主题字体，
 * 而是把生效主题解析出的 {@link FontId} 穿进度量——屏幕用主题覆盖（如
 * {@code GtsnScreen#setTheme}）时，度量 / 换行与 {@code GuiGraphicsRenderContext} 的渲染字体一致。
 * 单参构造 {@link #ThemeFontMetrics(Font)} 保留既有默认路径：绑定当前全局主题字体。
 * 度量逻辑委托给 MC-free 的 {@link ThemedTextMetrics}（可在无游戏环境测试）。</p>
 *
 * <p>客户端专用类：专职服务端不得加载（类加载纪律，见 ADR-0003/0004）。</p>
 */
public final class ThemeFontMetrics implements TextMetrics {

    private final ThemedTextMetrics delegate;

    /** 默认：绑定当前全局主题文本字体（既有默认路径）。 */
    public ThemeFontMetrics(Font font) {
        this(font, ThemeContext.active().textStyle().fontId());
    }

    /** 绑定屏幕生效主题（含屏幕级覆盖）的文本字体。 */
    public ThemeFontMetrics(Font font, Theme theme) {
        this(font, Objects.requireNonNull(theme, "theme").textStyle().fontId());
    }

    /** 绑定显式主题字体。 */
    public ThemeFontMetrics(Font font, FontId themeFont) {
        Objects.requireNonNull(font, "font");
        this.delegate = new ThemedTextMetrics(baseMetrics(font), Objects.requireNonNull(themeFont, "themeFont"));
    }

    /** 本度量绑定的主题字体。 */
    public FontId themeFont() {
        return delegate.themeFont();
    }

    @Override
    public int width(String text) {
        return delegate.width(text);
    }

    @Override
    public int width(String text, FontId fontId) {
        return delegate.width(text, fontId);
    }

    @Override
    public int lineHeight() {
        return delegate.lineHeight();
    }

    /** 底层 {@link Font} 度量：无字体 → 原版宽度；指定字体 → {@link ClientFonts}（含可用性回退）。 */
    private static TextMetrics baseMetrics(Font font) {
        return new TextMetrics() {
            @Override
            public int width(String text) {
                return font.width(text);
            }

            @Override
            public int width(String text, FontId fontId) {
                return ClientFonts.width(font, text, fontId);
            }

            @Override
            public int lineHeight() {
                return font.lineHeight;
            }
        };
    }
}
