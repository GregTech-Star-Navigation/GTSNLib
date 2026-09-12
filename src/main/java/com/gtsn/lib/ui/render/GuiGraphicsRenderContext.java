package com.gtsn.lib.ui.render;

import com.gtsn.lib.ui.client.ClientFonts;
import com.gtsn.lib.ui.theme.FontId;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/**
 * {@link RenderContext} 的 Minecraft 后端：把内核渲染抽象翻译为 {@link GuiGraphics} 调用。
 *
 * <p>客户端专用类：仅可由客户端代码（{@code ui.screen} / {@code ui.client}）引用，
 * 专职服务端不得加载（类加载纪律，见 ADR-0003/0004）。</p>
 *
 * <p>映射约定：{@link #blit} 以目标尺寸采样同尺寸纹理区域（不做源区域缩放）；
 * {@link #pushClip} 使用原版 scissor 坐标系（min/max）。</p>
 *
 * <p>字体（#23）：无字体的 {@code text} / {@code textWidth} / {@code centeredText} 使用
 * 当前主题文本字体（{@link Theme#textStyle()}.fontId()）；带 {@link FontId} 的重载为显式覆盖
 * （{@code null} = 原版默认）。字体经 {@code Style.withFont} 应用；资源缺失自动回退原版
 * （见 {@link ClientFonts}）。</p>
 */
public final class GuiGraphicsRenderContext implements RenderContext {

    private final GuiGraphics graphics;
    private final Font font;
    private final int width;
    private final int height;
    private final Theme theme;
    private final FontId themeFont;

    public GuiGraphicsRenderContext(GuiGraphics graphics, Font font, int width, int height) {
        this(graphics, font, width, height, ThemeContext.active());
    }

    public GuiGraphicsRenderContext(GuiGraphics graphics, Font font, int width, int height, Theme theme) {
        this.graphics = Objects.requireNonNull(graphics, "graphics");
        this.font = Objects.requireNonNull(font, "font");
        this.width = width;
        this.height = height;
        this.theme = Objects.requireNonNull(theme, "theme");
        this.themeFont = theme.textStyle().fontId();
    }

    /** 底层 {@link GuiGraphics}（供客户端专用扩展如物品渲染使用）。 */
    public GuiGraphics graphics() {
        return graphics;
    }

    /** 当前字体（供客户端专用扩展使用）。 */
    public Font font() {
        return font;
    }

    /** 当前主题文本字体（已解析；可能为原版哨兵）。 */
    public FontId themeFont() {
        return themeFont;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public int textWidth(String text) {
        return ClientFonts.width(font, text, themeFont);
    }

    @Override
    public int textWidth(String text, FontId fontId) {
        return ClientFonts.width(font, text, fontId);
    }

    @Override
    public int textLineHeight() {
        return font.lineHeight;
    }

    @Override
    public void fill(int x, int y, int w, int h, int argb) {
        graphics.fill(x, y, x + w, y + h, argb);
    }

    @Override
    public void fillGradient(int x, int y, int w, int h, int argbTop, int argbBottom) {
        graphics.fillGradient(x, y, x + w, y + h, argbTop, argbBottom);
    }

    @Override
    public void text(String text, int x, int y, int argb, boolean shadow) {
        renderText(text, x, y, argb, shadow, themeFont);
    }

    @Override
    public void text(String text, int x, int y, int argb, boolean shadow, FontId fontId) {
        renderText(text, x, y, argb, shadow, fontId);
    }

    /** 字体感知绘制：{@code requested} 不可应用（原版 / 资源缺失）时按无样式绘制。 */
    private void renderText(String text, int x, int y, int argb, boolean shadow, FontId requested) {
        graphics.drawString(font, ClientFonts.styled(text, requested), x, y, argb, shadow);
    }

    @Override
    public void blit(TextureRef texture, int x, int y, int w, int h, int u, int v,
                     int textureWidth, int textureHeight) {
        ResourceLocation location = new ResourceLocation(texture.namespace(), texture.path());
        graphics.blit(location, x, y, w, h, (float) u, (float) v, w, h, textureWidth, textureHeight);
    }

    @Override
    public Theme theme() {
        return theme;
    }

    @Override
    public boolean textureReady(TextureRef texture) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.getResourceManager() == null) {
            return false;
        }
        return minecraft.getResourceManager()
                .getResource(new ResourceLocation(texture.namespace(), texture.path()))
                .isPresent();
    }

    @Override
    public void pushClip(int x, int y, int w, int h) {
        graphics.enableScissor(x, y, x + w, y + h);
    }

    @Override
    public void popClip() {
        graphics.disableScissor();
    }

    @Override
    public void pushTranslate(float deltaX, float deltaY) {
        graphics.pose().pushPose();
        graphics.pose().translate(deltaX, deltaY, 0.0F);
    }

    @Override
    public void popTranslate() {
        graphics.pose().popPose();
    }
}
