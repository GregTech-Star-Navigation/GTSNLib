package com.gtsn.lib.ui.screen;

import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.render.GuiGraphicsRenderContext;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.widget.Widget;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Objects;

/**
 * GTSN UI 屏幕基类：持有控件树与 {@link WidgetHost}，把 Minecraft {@link Screen} 生命周期
 * 转发给纯逻辑内核（布局 / 渲染 / 输入）。
 *
 * <p>客户端专用类：仅可由客户端代码引用，专职服务端不得加载（类加载纪律，见 ADR-0003/0004）。</p>
 */
public class GtsnScreen extends Screen {

    private final Widget root;
    private WidgetHost host;

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

    @Override
    protected void init() {
        host = new WidgetHost(root);
        host.resize(width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        RenderContext context = new GuiGraphicsRenderContext(graphics, font, width, height);
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
