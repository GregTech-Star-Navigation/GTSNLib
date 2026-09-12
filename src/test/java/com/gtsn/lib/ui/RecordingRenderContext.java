package com.gtsn.lib.ui;

import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.render.TextureRef;

import java.util.ArrayList;
import java.util.List;

/**
 * 记录调用序列的测试渲染上下文：断言渲染顺序、裁剪与颜色选择，不依赖 Minecraft。
 */
public final class RecordingRenderContext implements RenderContext {

    public final List<String> ops = new ArrayList<>();

    private final int width;
    private final int height;

    public RecordingRenderContext(int width, int height) {
        this.width = width;
        this.height = height;
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
