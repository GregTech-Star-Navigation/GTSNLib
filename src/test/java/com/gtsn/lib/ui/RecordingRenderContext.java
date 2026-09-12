package com.gtsn.lib.ui;

import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.render.TextureRef;
import com.gtsn.lib.ui.theme.FontId;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeContext;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 记录调用序列的测试渲染上下文：断言渲染顺序、裁剪与颜色选择，不依赖 Minecraft。
 *
 * <p>主题可显式注入（默认跟随全局 {@link ThemeContext}）；可绘制纹理集合可预置，
 * 用于验证控件“有纹理用纹理、缺纹理回退纯色”的行为。</p>
 */
public final class RecordingRenderContext implements RenderContext {

    public final List<String> ops = new ArrayList<>();
    public final Set<TextureRef> readyTextures = new HashSet<>();

    private final int width;
    private final int height;
    private Theme theme = ThemeContext.active();

    public RecordingRenderContext(int width, int height) {
        this.width = width;
        this.height = height;
    }

    /** 固定本上下文使用的主题。 */
    public RecordingRenderContext theme(Theme theme) {
        this.theme = Objects.requireNonNull(theme, "theme");
        return this;
    }

    /** 声明纹理资源可绘制（供纹理回退路径测试）。 */
    public RecordingRenderContext withTexture(TextureRef texture) {
        readyTextures.add(Objects.requireNonNull(texture, "texture"));
        return this;
    }

    @Override
    public Theme theme() {
        return theme;
    }

    @Override
    public boolean textureReady(TextureRef texture) {
        return readyTextures.contains(texture);
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
        return text.length() * 6;
    }

    /** 字体感知宽度（#23）：自定义字体按 11px/字符记录，用于断言度量来源。 */
    @Override
    public int textWidth(String text, FontId font) {
        return font != null && !FontId.VANILLA.equals(font) ? text.length() * 11 : text.length() * 6;
    }

    @Override
    public int textLineHeight() {
        return 9;
    }

    @Override
    public void fill(int x, int y, int w, int h, int argb) {
        ops.add("fill(" + x + "," + y + "," + w + "," + h + "," + Integer.toHexString(argb) + ")");
    }

    @Override
    public void fillGradient(int x, int y, int w, int h, int argbTop, int argbBottom) {
        ops.add("gradient(" + x + "," + y + "," + w + "," + h + ","
                + Integer.toHexString(argbTop) + "," + Integer.toHexString(argbBottom) + ")");
    }

    @Override
    public void text(String text, int x, int y, int argb, boolean shadow) {
        ops.add("text(" + text + "@" + x + "," + y + "," + Integer.toHexString(argb) + ",shadow=" + shadow + ")");
    }

    /** 字体感知绘制（#23）：显式字体追加 {@code font=<id>} 供断言（null = 未指定）。 */
    @Override
    public void text(String text, int x, int y, int argb, boolean shadow, FontId font) {
        ops.add("text(" + text + "@" + x + "," + y + "," + Integer.toHexString(argb) + ",shadow=" + shadow
                + (font != null ? ",font=" + font : "") + ")");
    }

    @Override
    public void blit(TextureRef texture, int x, int y, int w, int h, int u, int v, int textureWidth, int textureHeight) {
        ops.add("blit(" + texture.location() + "," + x + "," + y + "," + w + "," + h + "," + u + "," + v + ")");
    }

    @Override
    public void pushClip(int x, int y, int w, int h) {
        ops.add("pushClip(" + x + "," + y + "," + w + "," + h + ")");
    }

    @Override
    public void popClip() {
        ops.add("popClip");
    }

    @Override
    public void pushTranslate(float deltaX, float deltaY) {
        ops.add("pushTranslate(" + deltaX + "," + deltaY + ")");
    }

    @Override
    public void popTranslate() {
        ops.add("popTranslate");
    }
}
