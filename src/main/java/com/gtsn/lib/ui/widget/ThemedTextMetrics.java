package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.theme.FontId;

import java.util.Objects;

/**
 * 把「生效主题字体」绑定到 {@link TextMetrics} 的 MC-free 适配器（#23 / #22 评审 F2-1 接缝修正）。
 *
 * <p>问题：原先客户端度量 {@code ThemeFontMetrics} 每次 {@link #width(String)} 都读全局
 * {@code ThemeContext.active()}，而渲染用的字体来自屏幕生效主题（{@code GtsnScreen#theme()}，
 * 含屏幕级覆盖）。屏幕覆盖主题字体后，度量 / 换行用全局字体、渲染用覆盖字体，二者不一致。</p>
 *
 * <p>修正：度量显式绑定一个 {@link FontId}（由屏幕生效主题解析而来），不再读全局；
 * 显式 {@link #width(String, FontId)}（逐控件覆盖）始终优先。默认（无覆盖）路径由调用方传入
 * 全局主题字体，行为与既有默认一致。</p>
 */
public final class ThemedTextMetrics implements TextMetrics {

    private final TextMetrics delegate;
    private final FontId themeFont;

    public ThemedTextMetrics(TextMetrics delegate, FontId themeFont) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.themeFont = Objects.requireNonNull(themeFont, "themeFont");
    }

    /** 绑定的主题字体。 */
    public FontId themeFont() {
        return themeFont;
    }

    @Override
    public int width(String text) {
        return delegate.width(text, themeFont);
    }

    @Override
    public int width(String text, FontId font) {
        return delegate.width(text, font);
    }

    @Override
    public int lineHeight() {
        return delegate.lineHeight();
    }
}
