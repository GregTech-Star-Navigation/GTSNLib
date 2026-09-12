package com.gtsn.lib.ui.render;

import com.gtsn.lib.ui.theme.ThemeContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 渲染抽象的 MC-free 契约扩展：主题入口默认跟随全局上下文、纹理可绘制性默认关闭、
 * 平铺绘制按格裁剪。
 */
class RenderContextTest {

    private static final TextureRef TEXTURE = TextureRef.of("gtsnlib", "textures/gui/panel_light.png");

    @AfterEach
    void resetTheme() {
        ThemeContext.reset();
    }

    /** 只记录纹理调用的最小渲染上下文。 */
    private static final class Recorder implements RenderContext {

        final List<String> blits = new ArrayList<>();

        @Override
        public int width() {
            return 100;
        }

        @Override
        public int height() {
            return 100;
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
        public void fill(int x, int y, int width, int height, int argb) {
        }

        @Override
        public void fillGradient(int x, int y, int width, int height, int argbTop, int argbBottom) {
        }

        @Override
        public void text(String text, int x, int y, int argb, boolean shadow) {
        }

        @Override
        public void blit(TextureRef texture, int x, int y, int width, int height, int u, int v,
                         int textureWidth, int textureHeight) {
            blits.add("blit(" + texture.location() + "," + x + "," + y + "," + width + "," + height
                    + "," + u + "," + v + ")");
        }

        @Override
        public void pushClip(int x, int y, int width, int height) {
        }

        @Override
        public void popClip() {
        }

        @Override
        public void pushTranslate(float deltaX, float deltaY) {
        }

        @Override
        public void popTranslate() {
        }
    }

    @Test
    void themeDefaultsToGlobalThemeContext() {
        ThemeContext.reset();
        assertSame(ThemeContext.active(), new Recorder().theme());
    }

    @Test
    void textureReadyDefaultsToFalse() {
        assertFalse(new Recorder().textureReady(TEXTURE), "无资源能力的上下文默认报告纹理不可绘制");
    }

    @Test
    void blitTiledCoversAreaWithFullAndPartialTiles() {
        Recorder recorder = new Recorder();

        recorder.blitTiled(TEXTURE, 2, 3, 20, 10, 16);

        assertEquals(List.of(
                "blit(gtsnlib:textures/gui/panel_light.png,2,3,16,10,0,0)",
                "blit(gtsnlib:textures/gui/panel_light.png,18,3,4,10,0,0)"), recorder.blits);
    }

    @Test
    void blitTiledSkipsEmptyArea() {
        Recorder recorder = new Recorder();
        recorder.blitTiled(TEXTURE, 0, 0, 0, 10, 16);
        assertTrue(recorder.blits.isEmpty());
    }

    @Test
    void blitTiledRejectsNonPositiveTileSize() {
        Recorder recorder = new Recorder();
        assertThrows(IllegalArgumentException.class, () -> recorder.blitTiled(TEXTURE, 0, 0, 8, 8, 0));
    }
}
