package com.gtsn.lib.ui.client;

import com.gtsn.lib.ui.theme.FontId;
import com.gtsn.lib.ui.theme.ThemeContext;
import com.gtsn.lib.ui.widget.TextMetrics;
import net.minecraft.client.gui.Font;

import java.util.Objects;

/**
 * 客户端文本度量（#23）：布局测量与渲染共用同一字体语义——
 * 未指定字体时取当前主题文本字体（{@link ThemeContext#active()}），
 * 指定字体时按 {@link ClientFonts} 应用（资源缺失回退原版）。
 *
 * <p>使 GTSN UI 的固有尺寸 / 换行宽度与 {@code Font.width} 渲染宽度一致；
 * 无字体依赖的 GameTest / 服务端继续用 {@code PlainTextMetrics}。</p>
 *
 * <p>客户端专用类：专职服务端不得加载（类加载纪律，见 ADR-0003/0004）。</p>
 */
public final class ThemeFontMetrics implements TextMetrics {

    private final Font font;

    public ThemeFontMetrics(Font font) {
        this.font = Objects.requireNonNull(font, "font");
    }

    @Override
    public int width(String text) {
        return ClientFonts.width(font, text, ThemeContext.active().textStyle().fontId());
    }

    @Override
    public int width(String text, FontId fontId) {
        return ClientFonts.width(font, text, fontId);
    }

    @Override
    public int lineHeight() {
        return font.lineHeight;
    }
}
