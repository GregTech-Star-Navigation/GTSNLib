package com.gtsn.lib.ui.render;

/**
 * 控件渲染抽象：控件只依赖本接口，具体实现（如基于 Minecraft {@code GuiGraphics}）在客户端侧提供。
 *
 * <p>坐标单位为像素（GUI 缩放后坐标系）；{@code argb} 为 0xAARRGGBB。</p>
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
}
