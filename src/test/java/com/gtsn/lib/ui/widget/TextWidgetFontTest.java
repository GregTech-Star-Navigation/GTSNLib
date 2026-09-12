package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.screen.WidgetHost;
import com.gtsn.lib.ui.theme.FontId;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 文本控件的字体接线（#23，无 MC）：逐控件字体覆盖同时驱动**度量**（换行 / 固有尺寸）与**渲染**，
 * 未覆盖时字体选择交给渲染上下文（客户端取主题字体）。
 */
class TextWidgetFontTest {

    private static final FontId CUSTOM = FontId.of("gtsnlib", "sarasa_ui_sc");

    /** 自定义字体 11px/字符，原版 6px/字符，用于区分度量来源。 */
    private static final class FontSensitiveMetrics implements TextMetrics {
        @Override
        public int width(String text) {
            return text.length() * 6;
        }

        @Override
        public int width(String text, FontId font) {
            return font != null && !FontId.VANILLA.equals(font) ? text.length() * 11 : text.length() * 6;
        }

        @Override
        public int lineHeight() {
            return 9;
        }
    }

    private static final TextMetrics METRICS = new FontSensitiveMetrics();

    @Test
    void customFontDrivesWrappingAndIntrinsicSize() {
        Stack root = Stack.vertical();
        TextWidget text = root.add(new TextWidget("abcd", METRICS).font(CUSTOM).wrap(24));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);

        assertEquals(List.of("ab", "cd"), text.lines(), "自定义字体 11px/字符：24px 宽只放得下 2 字符");
        assertEquals(Rect.of(0, 0, 22, 18), text.bounds(), "固有尺寸来自自定义字体度量");
    }

    @Test
    void vanillaOverrideRestoresBaseMetrics() {
        Stack root = Stack.vertical();
        TextWidget text = root.add(new TextWidget("abcd", METRICS).vanillaFont().wrap(24));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);

        assertEquals(List.of("abcd"), text.lines(), "原版覆盖回退基础度量 6px/字符：abcd=24px 恰好容纳");
        assertEquals(Rect.of(0, 0, 24, 9), text.bounds());
    }

    @Test
    void withoutOverrideFontSelectionIsDelegatedToContext() {
        Stack root = Stack.vertical();
        TextWidget text = root.add(new TextWidget("abcd", METRICS).wrap(24));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);

        assertEquals(List.of("abcd"), text.lines());
        assertEquals(Optional.empty(), text.fontOverride());
    }

    @Test
    void renderPassesExplicitFontToContext() {
        Stack root = Stack.vertical();
        root.add(new TextWidget("ab", METRICS).font(CUSTOM));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);
        RecordingRenderContext ctx = new RecordingRenderContext(200, 100);

        host.render(ctx);

        assertTrue(ctx.ops.contains("text(ab@0,0,ffe6e6e6,shadow=false,font=gtsnlib:sarasa_ui_sc)"),
                "显式字体随绘制调用传入: " + ctx.ops);
    }

    @Test
    void renderVanillaOverridePassesVanillaSentinel() {
        Stack root = Stack.vertical();
        root.add(new TextWidget("ab", METRICS).vanillaFont());
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);
        RecordingRenderContext ctx = new RecordingRenderContext(200, 100);

        host.render(ctx);

        assertTrue(ctx.ops.contains("text(ab@0,0,ffe6e6e6,shadow=false,font=minecraft:default)"),
                "显式原版字体为哨兵而非缺省: " + ctx.ops);
    }

    @Test
    void renderWithoutOverrideLeavesFontToContext() {
        Stack root = Stack.vertical();
        root.add(new TextWidget("ab", METRICS));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);
        RecordingRenderContext ctx = new RecordingRenderContext(200, 100);

        host.render(ctx);

        assertTrue(ctx.ops.contains("text(ab@0,0,ffe6e6e6,shadow=false)"),
                "无覆盖时不携带字体，由渲染上下文（主题）决定: " + ctx.ops);
    }

    @Test
    void centeringUsesFontAwareWidthForOverride() {
        Stack root = Stack.vertical();
        root.add(new TextWidget("ab", METRICS)
                .font(CUSTOM)
                .size(Sizing.fixed(60), Sizing.wrap())
                .align(TextAlign.CENTER));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);
        RecordingRenderContext ctx = new RecordingRenderContext(200, 100);

        host.render(ctx);

        // 自定义字体宽度 22px → 居中起点 (60-22)/2 = 19
        assertTrue(ctx.ops.contains("text(ab@19,0,ffe6e6e6,shadow=false,font=gtsnlib:sarasa_ui_sc)"),
                "居中对齐使用字体感知宽度: " + ctx.ops);
    }
}
