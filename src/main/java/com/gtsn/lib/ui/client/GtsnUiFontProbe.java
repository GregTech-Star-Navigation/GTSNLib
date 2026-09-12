package com.gtsn.lib.ui.client;

import com.gtsn.lib.ui.theme.FontId;
import com.gtsn.lib.ui.theme.ThemeContext;
import net.minecraft.client.gui.Font;

import java.util.Objects;

/**
 * 自定义字体运行时探针（#23，客户端专用）：以**实际渲染宽度**为正向证据，证明主题字体真的被 MC 的
 * {@code FontSet} 采用，而不是被 {@link ClientFonts#effective(FontId)} 安全回退静默吞掉。
 *
 * <p>背景（#23 复检）：{@code ClientFonts} 的安全回退会在字体定义 / 引用文件不可加载时改为原版渲染，
 * 因此“截图无豆腐块”**不能**证明自定义字体生效——它也可能只是回退后的原版像素字。本探针比较同一段
 * 中英混排样例在「主题字体」与「原版字体」下的 {@code Font.width}：只有两者不同，才说明渲染路径确实
 * 换了字库。自动测试据此断言，回归无需肉眼比对截图即可被捕获。</p>
 *
 * <p>客户端专用类：专职服务端不得加载（类加载纪律，见 ADR-0003/0004）。</p>
 */
public final class GtsnUiFontProbe {

    /** 中英混排样例：覆盖 TTF 子集的 Latin/CJK，并以子集外字符「龘」验证 reference 回退层。 */
    public static final String SAMPLE = "GTSN UI 中文 English 0123 龘";

    private GtsnUiFontProbe() {
    }

    /**
     * 探针结果：请求字体、实际生效字体，以及主题 / 原版两种度量。
     *
     * @param requested   主题（或显式）请求的字体 id
     * @param effective   实际生效字体 id；{@code null} 表示回退原版（未套 {@code Style.withFont}）
     * @param themeWidth  {@link #SAMPLE} 在请求字体下的宽度
     * @param vanillaWidth{@link #SAMPLE} 在原版默认字体下的宽度
     */
    public record Result(FontId requested, FontId effective, int themeWidth, int vanillaWidth) {

        /** 自定义字体是否真的生效（未被安全回退吞掉）。 */
        public boolean customApplied() {
            return effective != null;
        }

        /** 主题字体宽度是否与原版不同（证明渲染路径确实换了字库）。 */
        public boolean widthDiffers() {
            return themeWidth != vanillaWidth;
        }

        /** 两项都满足才算通过。 */
        public boolean verified() {
            return customApplied() && widthDiffers();
        }

        /** 单行描述，供日志 / 异常信息使用。 */
        public String describe() {
            return "requested=" + requested.location()
                    + ", effective=" + (effective == null ? "minecraft:default(fallback)" : effective.location())
                    + ", themeWidth=" + themeWidth
                    + ", vanillaWidth=" + vanillaWidth
                    + ", delta=" + (themeWidth - vanillaWidth);
        }
    }

    /** 仅测量、不抛异常（供日志 / 诊断）。 */
    public static Result measure(Font font, FontId requested) {
        Objects.requireNonNull(font, "font");
        Objects.requireNonNull(requested, "requested");
        FontId applied = ClientFonts.effective(requested);
        int themeWidth = ClientFonts.width(font, SAMPLE, requested);
        int vanillaWidth = font.width(SAMPLE);
        return new Result(requested, applied, themeWidth, vanillaWidth);
    }

    /**
     * 断言主题字体确实生效；否则抛出 {@link IllegalStateException}。
     * 自动测试据此失败，回归无需肉眼比对截图。
     */
    public static Result verify(Font font, FontId requested) {
        Result result = measure(font, requested);
        if (!result.verified()) {
            throw new IllegalStateException("[GTSNLib] 主题字体未生效，疑似回退原版: " + result.describe());
        }
        return result;
    }

    /** 以当前激活主题的文本字体做一次断言探针（自动测试入口）。 */
    public static Result verifyActiveTheme(Font font) {
        return verify(font, ThemeContext.active().textStyle().fontId());
    }
}
