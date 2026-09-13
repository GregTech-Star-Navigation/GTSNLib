package com.gtsn.lib.ui.screen;

import com.gtsn.lib.ui.client.ThemeFontMetrics;
import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.render.GuiGraphicsRenderContext;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeColorRole;
import com.gtsn.lib.ui.theme.ThemeContext;
import com.gtsn.lib.ui.widget.TextMetrics;
import com.gtsn.lib.ui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * GTSN UI 屏幕基类：持有控件树与 {@link WidgetHost}，把 Minecraft {@link Screen} 生命周期
 * 转发给纯逻辑内核（布局 / 渲染 / 输入）。
 *
 * <p>主题（#18）：渲染上下文携带当前主题（默认跟随 {@link ThemeContext}，可用
 * {@link #setTheme(Theme)} 做屏幕级覆盖），背景色取主题 {@code BACKGROUND} 角色。</p>
 *
 * <p>客户端专用类：仅可由客户端代码引用，专职服务端不得加载（类加载纪律，见 ADR-0003/0004）。</p>
 */
public class GtsnScreen extends Screen {

    private Widget root;
    private WidgetHost host;
    private Theme themeOverride;

    public GtsnScreen(Component title, Widget root) {
        super(title);
        this.root = Objects.requireNonNull(root, "root");
    }

    /** 屏幕打开期间内核泊点（init 后可用）。 */
    public WidgetHost host() {
        if (host == null) {
            host = new WidgetHost(root);
            host.resize(width, height);
        }
        return host;
    }

    /** 屏幕级主题覆盖；{@code null}（默认）表示跟随 {@link ThemeContext} 当前主题。 */
    public void setTheme(Theme theme) {
        this.themeOverride = theme;
        onThemeChanged();
    }

    /**
     * 屏幕级主题变更钩子：子类可据此用新主题重建控件树，使文本度量绑定到新主题字体。
     * 默认空实现（无构造期度量的屏幕无需处理）。
     */
    protected void onThemeChanged() {
    }

    /**
     * 文本度量：绑定**本屏生效主题**（含 {@link #setTheme} 覆盖）的字体，使布局测量 / 换行与
     * {@link #render} 中 {@link GuiGraphicsRenderContext} 的渲染字体一致（#22 评审 F2-1 接缝修正）。
     */
    protected TextMetrics textMetrics() {
        return new ThemeFontMetrics(Minecraft.getInstance().font, theme());
    }

    /** 当前生效主题（渲染上下文与背景绘制共用）。 */
    public Theme theme() {
        return themeOverride != null ? themeOverride : ThemeContext.active();
    }

    /** 替换根控件树（主题切换重建等场景）；下一次布局（host 重建）生效。 */
    protected void setRoot(Widget root) {
        this.root = Objects.requireNonNull(root, "root");
        this.host = null;
    }

    @Override
    protected void init() {
        host = new WidgetHost(root);
        host.resize(width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        Theme theme = theme();
        int background = theme.color(ThemeColorRole.BACKGROUND);
        if ((background >>> 24) != 0 && width > 0 && height > 0) {
            graphics.fill(0, 0, width, height, background);
        }
        RenderContext context = new GuiGraphicsRenderContext(graphics, font, width, height, theme);
        host().render(context);
        host().renderTooltips(context);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        host().dispatch(new InputEvent.MouseMoved(mouseX, mouseY));
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (host().dispatch(new InputEvent.MousePressed(mouseX, mouseY, button))) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (host().dispatch(new InputEvent.MouseReleased(mouseX, mouseY, button))) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (host().dispatch(new InputEvent.MouseDragged(mouseX, mouseY, button, dragX, dragY))) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (host().dispatch(new InputEvent.MouseScrolled(mouseX, mouseY, 0.0, scrollDelta))) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollDelta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (host().dispatch(new InputEvent.KeyPressed(keyCode, scanCode, modifiers))) {
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (host().dispatch(new InputEvent.KeyReleased(keyCode, scanCode, modifiers))) {
            return true;
        }
        return super.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (host().dispatch(new InputEvent.CharTyped(codePoint, modifiers))) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
