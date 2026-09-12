package com.gtsn.lib.ui.input;

import com.gtsn.lib.ui.layout.Anchor;
import com.gtsn.lib.ui.layout.LayoutNode;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.widget.Widget;

import java.util.ArrayList;
import java.util.List;

/**
 * 输入路由测试替身：记录收到的事件与状态回调，可配置消费/可聚焦/裁剪行为。
 */
final class ProbeWidget implements Widget {

    private final String name;
    private final List<String> log;
    private final LayoutNode node = new LayoutNode();
    private final List<Widget> children = new ArrayList<>();
    private boolean focusable;
    private boolean consume = true;
    private boolean clipsChildren;

    ProbeWidget(String name, List<String> log) {
        this.name = name;
        this.log = log;
    }

    ProbeWidget fixed(int width, int height) {
        node.params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    ProbeWidget absoluteTopLeft() {
        node.params().absolute(Anchor.TOP_LEFT);
        return this;
    }

    ProbeWidget focusable() {
        this.focusable = true;
        return this;
    }

    ProbeWidget silent() {
        this.consume = false;
        return this;
    }

    ProbeWidget consume(boolean value) {
        this.consume = value;
        return this;
    }

    ProbeWidget clipping() {
        this.clipsChildren = true;
        return this;
    }

    ProbeWidget add(ProbeWidget... widgets) {
        for (ProbeWidget child : widgets) {
            children.add(child);
            node.addChild(child.node());
        }
        return this;
    }

    @Override
    public LayoutNode node() {
        return node;
    }

    @Override
    public List<Widget> children() {
        return children;
    }

    @Override
    public boolean onInput(InputEvent event) {
        log.add(name + ":" + label(event));
        return consume;
    }

    @Override
    public boolean isFocusable() {
        return focusable;
    }

    @Override
    public boolean clipsChildren() {
        return clipsChildren;
    }

    @Override
    public void onFocusChanged(boolean focused) {
        log.add(name + ":focus=" + focused);
    }

    @Override
    public void onHoverChanged(boolean hovered) {
        log.add(name + ":hover=" + hovered);
    }

    static String label(InputEvent event) {
        if (event instanceof InputEvent.MouseMoved) {
            return "move";
        }
        if (event instanceof InputEvent.MousePressed) {
            return "press";
        }
        if (event instanceof InputEvent.MouseReleased) {
            return "release";
        }
        if (event instanceof InputEvent.MouseDragged) {
            return "drag";
        }
        if (event instanceof InputEvent.MouseScrolled) {
            return "scroll";
        }
        if (event instanceof InputEvent.KeyPressed) {
            return "key";
        }
        if (event instanceof InputEvent.KeyReleased) {
            return "keyUp";
        }
        if (event instanceof InputEvent.CharTyped) {
            return "char";
        }
        return "unknown";
    }
}
