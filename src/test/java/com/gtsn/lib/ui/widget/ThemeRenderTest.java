package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.render.TextureRef;
import com.gtsn.lib.ui.screen.WidgetHost;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeColorRole;
import com.gtsn.lib.ui.theme.ThemeDefinition;
import com.gtsn.lib.ui.theme.ThemeId;
import com.gtsn.lib.ui.theme.ThemeResolver;
import com.gtsn.lib.ui.theme.ThemeTextureRole;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 控件默认颜色 / 度量从渲染上下文的主题取值；显式字面量覆盖仍优先。
 *
 * <p>主题切换可见生效的 MC-free 证据：同一控件树、两个主题上下文，绘制颜色不同。</p>
 */
class ThemeRenderTest {

    private static final ThemeId CUSTOM_ID = ThemeId.of("gtsnlib", "test");

    private static Theme customTheme() {
        return ThemeResolver.resolve(List.of(ThemeDefinition.builder(CUSTOM_ID)
                .color(ThemeColorRole.TEXT, 0xFF00FF00)
                .color(ThemeColorRole.BUTTON_BACKGROUND, 0xFF102030)
                .color(ThemeColorRole.BUTTON_BACKGROUND_HOVERED, 0xFF203040)
                .color(ThemeColorRole.TOOLTIP_BACKGROUND, 0xFF010203)
                .color(ThemeColorRole.TOOLTIP_BORDER, 0xFF040506)
                .color(ThemeColorRole.TOOLTIP_TEXT, 0xFF070809)
                .build())).get(CUSTOM_ID);
    }

    private static boolean hasOp(RecordingRenderContext ctx, String fragment) {
        return ctx.ops.stream().anyMatch(op -> op.contains(fragment));
    }

    @Test
    void textWidgetUsesThemeColorWhenNotOverridden() {
        Stack root = Stack.vertical().padding(Insets.all(2));
        root.add(new TextWidget("hi", PlainTextMetrics.INSTANCE));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);
        RecordingRenderContext ctx = new RecordingRenderContext(100, 50).theme(customTheme());

        host.render(ctx);

        assertTrue(hasOp(ctx, "ff00ff00"), "未显式指定颜色时应取主题 TEXT 角色: " + ctx.ops);
    }

    @Test
    void explicitColorOverridesTheme() {
        Stack root = Stack.vertical().padding(Insets.all(2));
        root.add(new TextWidget("hi", PlainTextMetrics.INSTANCE).color(0xFF123456));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);
        RecordingRenderContext ctx = new RecordingRenderContext(100, 50).theme(customTheme());

        host.render(ctx);

        assertTrue(hasOp(ctx, "ff123456"), "显式字面量优先于主题角色: " + ctx.ops);
        assertFalse(hasOp(ctx, "ff00ff00"), "覆盖后不再取主题色: " + ctx.ops);
    }

    @Test
    void buttonFillAndHoverUseThemeRoles() {
        Stack root = Stack.vertical();
        root.add(new ButtonWidget("ok", PlainTextMetrics.INSTANCE, () -> {
        }).fixedSize(40, 20));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);
        RecordingRenderContext ctx = new RecordingRenderContext(100, 50).theme(customTheme());

        host.render(ctx);
        assertTrue(hasOp(ctx, "ff102030"), "常规态取主题按钮底色: " + ctx.ops);

        host.dispatch(new InputEvent.MouseMoved(10, 10));
        ctx.ops.clear();
        host.render(ctx);
        assertTrue(hasOp(ctx, "ff203040"), "悬停态取主题按钮悬停角色: " + ctx.ops);
    }

    @Test
    void tooltipUsesThemeRoles() {
        Stack root = Stack.vertical();
        ButtonWidget button = root.add(new ButtonWidget("ok", PlainTextMetrics.INSTANCE, () -> {
        }).fixedSize(60, 20).tooltip(Tooltip.of("tip")));
        WidgetHost host = new WidgetHost(root);
        host.resize(200, 100);
        host.dispatch(new InputEvent.MouseMoved(button.bounds().x() + 5, button.bounds().y() + 5));
        RecordingRenderContext ctx = new RecordingRenderContext(200, 100).theme(customTheme());

        host.renderTooltips(ctx);

        assertTrue(hasOp(ctx, "ff010203"), "提示背景取主题角色: " + ctx.ops);
        assertTrue(hasOp(ctx, "ff040506"), "提示边框取主题角色: " + ctx.ops);
        assertTrue(hasOp(ctx, "ff070809"), "提示文字取主题角色: " + ctx.ops);
    }

    @Test
    void sameTreeRendersDifferentColorsUnderDifferentThemes() {
        Stack root = Stack.vertical().padding(Insets.all(2));
        root.add(new TextWidget("hi", PlainTextMetrics.INSTANCE));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);

        RecordingRenderContext light = new RecordingRenderContext(100, 50).theme(customTheme());
        RecordingRenderContext base = new RecordingRenderContext(100, 50).theme(
                ThemeResolver.resolve(List.of()).defaultTheme());
        host.render(light);
        host.render(base);

        assertTrue(hasOp(light, "ff00ff00"), light.ops.toString());
        assertFalse(hasOp(base, "ff00ff00"), "默认主题不产生自定义主题色: " + base.ops);
    }

    @Test
    void panelRoleBackgroundBorderAndTextureFollowTheme() {
        ThemeId panelTexture = ThemeId.of("gtsnlib", "textures/gui/panel_light.png");
        Theme theme = ThemeResolver.resolve(List.of(ThemeDefinition.builder(ThemeId.of("gtsnlib", "t"))
                .color(ThemeColorRole.PANEL_BACKGROUND, 0xFF0A0B0C)
                .color(ThemeColorRole.PANEL_BORDER, 0xFF0D0E0F)
                .texture(ThemeTextureRole.PANEL, panelTexture)
                .build())).get(ThemeId.of("gtsnlib", "t"));

        Stack root = Stack.vertical();
        root.add(new PanelWidget().background(ThemeColorRole.PANEL_BACKGROUND)
                .border(ThemeColorRole.PANEL_BORDER, 1).fixedSize(40, 20));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);

        RecordingRenderContext noTexture = new RecordingRenderContext(100, 50).theme(theme);
        host.render(noTexture);
        assertTrue(hasOp(noTexture, "fill(0,0,40,20,ff0a0b0c)"), "角色背景解析主题色: " + noTexture.ops);
        assertTrue(hasOp(noTexture, "fill(0,0,40,1,ff0d0e0f)"), "角色边框解析主题色: " + noTexture.ops);
        assertFalse(hasOp(noTexture, "blit("), "纹理不可用时回退纯色: " + noTexture.ops);

        RecordingRenderContext textured = new RecordingRenderContext(100, 50).theme(theme);
        textured.withTexture(TextureRef.of("gtsnlib", "textures/gui/panel_light.png"));
        host.render(textured);
        assertTrue(hasOp(textured, "blit(gtsnlib:textures/gui/panel_light.png"), "纹理可用时平铺绘制: " + textured.ops);
    }

    @Test
    void panelWithoutBackgroundStaysTransparent() {
        Stack root = Stack.vertical();
        PanelWidget panel = root.add(new PanelWidget().fixedSize(40, 20));
        panel.add(new BoxWidget().fixedSize(10, 10).fill(0xFF010203));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);
        RecordingRenderContext ctx = new RecordingRenderContext(100, 50).theme(customTheme());

        host.render(ctx);

        assertTrue(ctx.ops.get(0).equals("fill(0,0,10,10,ff010203)"),
                "未声明背景的面板保持透明（既有契约）: " + ctx.ops);
    }

    @Test
    void progressBarGradientAndTrackFollowTheme() {
        Theme theme = ThemeResolver.resolve(List.of(ThemeDefinition.builder(ThemeId.of("gtsnlib", "t"))
                .color(ThemeColorRole.PROGRESS_TRACK, 0xFF778899)
                .color(ThemeColorRole.PROGRESS_FILL, 0xFF112233)
                .color(ThemeColorRole.PROGRESS_FILL_TOP, 0xFF445566)
                .build())).get(ThemeId.of("gtsnlib", "t"));

        Stack root = Stack.vertical();
        root.add(new ProgressBarWidget().range(0, 1).value(0.5).gradient(true).fixedSize(40, 12));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);
        RecordingRenderContext ctx = new RecordingRenderContext(100, 50).theme(theme);

        host.render(ctx);

        assertTrue(hasOp(ctx, "fill(0,0,40,12,ff778899)"), "进度轨道取主题角色: " + ctx.ops);
        assertTrue(hasOp(ctx, "gradient(1,1,"), "渐变填充存在: " + ctx.ops);
        assertTrue(hasOp(ctx, "ff445566,ff112233"), "渐变顶/底色取主题角色: " + ctx.ops);
    }

    @Test
    void dividerAndBoxRolesFollowTheme() {
        Theme theme = ThemeResolver.resolve(List.of(ThemeDefinition.builder(ThemeId.of("gtsnlib", "t"))
                .color(ThemeColorRole.DIVIDER, 0xFFAB0000)
                .color(ThemeColorRole.ACCENT, 0xFF00AAFF)
                .build())).get(ThemeId.of("gtsnlib", "t"));

        Stack root = Stack.vertical().padding(Insets.all(2)).gap(2);
        root.add(DividerWidget.horizontal());
        root.add(new BoxWidget().fill(ThemeColorRole.ACCENT).fixedSize(20, 10));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);
        RecordingRenderContext ctx = new RecordingRenderContext(100, 50).theme(theme);

        host.render(ctx);

        assertTrue(hasOp(ctx, "ffab0000"), "分隔线取主题 DIVIDER 角色: " + ctx.ops);
        assertTrue(hasOp(ctx, "ff00aaff"), "色盒取主题 ACCENT 角色: " + ctx.ops);
    }

    @Test
    void itemSlotBackgroundAndSelectionFollowTheme() {
        Theme theme = ThemeResolver.resolve(List.of(ThemeDefinition.builder(ThemeId.of("gtsnlib", "t"))
                .color(ThemeColorRole.SLOT_BACKGROUND, 0xFF0F0E0D)
                .color(ThemeColorRole.SLOT_SELECTED_BORDER, 0xFF00FF00)
                .build())).get(ThemeId.of("gtsnlib", "t"));

        Stack root = Stack.vertical();
        root.add(new ItemSlotWidget().selectable(true).selected(true).fixedSize(18, 18));
        WidgetHost host = new WidgetHost(root);
        host.resize(100, 50);
        RecordingRenderContext ctx = new RecordingRenderContext(100, 50).theme(theme);

        host.render(ctx);

        assertTrue(hasOp(ctx, "fill(0,0,18,18,ff0f0e0d)"), "槽底取主题角色: " + ctx.ops);
        assertTrue(hasOp(ctx, "ff00ff00"), "选中边框取主题角色: " + ctx.ops);
    }
}
