package com.gtsn.lib.ui.render;

import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeContext;

/**
 * 控件渲染抽象：控件只依赖本接口，具体实现（如基于 Minecraft {@code GuiGraphics}）在客户端侧提供。
 *
 * <p>坐标单位为像素（GUI 缩放后坐标系）；{@code argb} 为 0xAARRGGBB。</p>
 *
 * <p>主题接入（#18）：控件默认颜色 / 度量经 {@link #theme()} 取值（缺省跟随
 * {@link ThemeContext} 当前主题）；纹理型绘制先经 {@link #textureReady(TextureRef)}
 * 判定可绘制性，缺纹理时回退纯色。</p>
 */
public interface RenderContext {

    int width();

    int height();

    /** 文本渲染宽度（用于固有尺寸测量与居中）。 */
    int textWidth(String text);

    /** 单行文本高度。 */
    int textLineHeight();

    void fill(int x, int y, int width, int height, int argb);

    /** 自上而下的线性渐变填充。 */
    void fillGradient(int x, int y, int width, int height, int argbTop, int argbBottom);

    void text(String text, int x, int y, int argb, boolean shadow);

    /** 以 centerX 为中心的水平居中文本。 */
    default void centeredText(String text, int centerX, int y, int argb, boolean shadow) {
        text(text, centerX - textWidth(text) / 2, y, argb, shadow);
    }

    /**
     * 纹理拷贝：把纹理 {@code (u, v)} 起的 {@code width x height} 区域绘制到 {@code (x, y)}，
     * 源纹理尺寸由 {@code textureWidth/textureHeight} 提供。
     */
    void blit(TextureRef texture, int x, int y, int width, int height, int u, int v,
              int textureWidth, int textureHeight);

    /** 裁剪入栈：仅显示与给定矩形相交的后续绘制内容。 */
    void pushClip(int x, int y, int width, int height);

    /** 裁剪出栈，与 {@link #pushClip} 配对。 */
    void popClip();

    /** 平移变换入栈（仅影响渲染，不影响输入路由）。 */
    void pushTranslate(float deltaX, float deltaY);

    /** 平移变换出栈，与 {@link #pushTranslate} 配对。 */
    void popTranslate();

    /**
     * 当前主题：控件在未显式指定颜色时按语义角色取值。默认跟随全局 {@link ThemeContext}
     * （切换主题后下一帧生效）；具体后端可携带屏幕级覆盖。
     */
    default Theme theme() {
        return ThemeContext.active();
    }

    /**
     * 纹理是否可绘制（资源存在且可加载）。默认 {@code false}：无资源能力的上下文
     * （单测 / GameTest / 服务端）使控件回退到纯色绘制。
     */
    default boolean textureReady(TextureRef texture) {
        return false;
    }

    /**
     * 以 {@code tileSize} 平铺纹理到目标矩形；不足一格的边缘绘制裁剪后的部分格。
     * 默认实现逐格调用 {@link #blit}，对任何后端都成立（客户端可覆写为更高效的批绘制）。
     *
     * @throws IllegalArgumentException {@code tileSize <= 0}
     */
    default void blitTiled(TextureRef texture, int x, int y, int width, int height, int tileSize) {
        if (tileSize <= 0) {
            throw new IllegalArgumentException("tile size must be positive: " + tileSize);
        }
        if (texture == null || width <= 0 || height <= 0) {
            return;
        }
        for (int tileY = 0; tileY < height; tileY += tileSize) {
            int tileHeight = Math.min(tileSize, height - tileY);
            for (int tileX = 0; tileX < width; tileX += tileSize) {
                int tileWidth = Math.min(tileSize, width - tileX);
                blit(texture, x + tileX, y + tileY, tileWidth, tileHeight, 0, 0, tileSize, tileSize);
            }
        }
    }
}
