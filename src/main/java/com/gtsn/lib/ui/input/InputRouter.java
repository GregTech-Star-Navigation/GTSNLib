package com.gtsn.lib.ui.input;

import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.widget.Widget;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 输入路由器：对控件树做命中测试，并把 {@link InputEvent} 派发给目标控件（向上冒泡至消费为止）。
 *
 * <p>语义约定：</p>
 * <ul>
 *   <li>命中测试按深度优先、后添加子节点优先；被裁剪的子树不可命中。</li>
 *   <li>鼠标按下：命中目标的最近可聚焦祖先获得焦点；无则可聚焦目标时清除焦点；
 *       目标成为拖拽捕获者，拖拽与释放均派发给它（即使指针移出包围盒）。</li>
 *   <li>键盘/字符：优先派发给焦点控件；无焦点时派发给根控件；Tab 未被消费时循环切换焦点。</li>
 * </ul>
 */
public final class InputRouter {

    static final Rect UNBOUNDED_CLIP = Rect.of(-1_000_000, -1_000_000, 2_000_000, 2_000_000);

    private final FocusManager focus = new FocusManager();
    private Widget root;
    private Widget hovered;
    private Widget pressed;
    private List<Widget> pressedPath = List.of();

    public InputRouter(Widget root) {
        setRoot(root);
    }

    public Widget root() {
        return root;
    }

    public FocusManager focus() {
        return focus;
    }

    public Optional<Widget> hovered() {
        return Optional.ofNullable(hovered);
    }

    public Optional<Widget> pressedWidget() {
        return Optional.ofNullable(pressed);
    }

    /** 更换控件树：重置悬停/拖拽状态并重建焦点顺序。 */
    public void setRoot(Widget root) {
        this.root = Objects.requireNonNull(root, "root");
        this.hovered = null;
        this.pressed = null;
        this.pressedPath = List.of();
        this.focus.setFocusOrder(collectFocusable(root));
    }

    /** 深度优先命中测试；返回最深层命中的控件。 */
    public Optional<Widget> hitTest(double x, double y) {
        List<Widget> path = hitPath(x, y);
        return path.isEmpty() ? Optional.empty() : Optional.of(path.get(path.size() - 1));
    }

    /** 派发输入事件；返回事件是否被消费。 */
    public boolean dispatch(InputEvent event) {
        Objects.requireNonNull(event, "event");
        if (event instanceof InputEvent.MouseMoved moved) {
            return dispatchMouseMoved(moved);
        }
        if (event instanceof InputEvent.MousePressed pressed) {
            return dispatchMousePressed(pressed);
        }
        if (event instanceof InputEvent.MouseReleased released) {
            return dispatchMouseReleased(released);
        }
        if (event instanceof InputEvent.MouseDragged dragged) {
            return dispatchMouseDragged(dragged);
        }
        if (event instanceof InputEvent.MouseScrolled scrolled) {
            return dispatchMouseScrolled(scrolled);
        }
        if (event instanceof InputEvent.KeyPressed keyPressed) {
            return dispatchKeyPressed(keyPressed);
        }
        if (event instanceof InputEvent.KeyReleased keyReleased) {
            return dispatchKeyReleased(keyReleased);
        }
        if (event instanceof InputEvent.CharTyped charTyped) {
            return dispatchCharTyped(charTyped);
        }
        return false;
    }

    private boolean dispatchMouseMoved(InputEvent.MouseMoved event) {
        List<Widget> path = hitPath(event.x(), event.y());
        updateHover(path.isEmpty() ? null : path.get(path.size() - 1));
        return bubble(path, event);
    }

    private boolean dispatchMousePressed(InputEvent.MousePressed event) {
        List<Widget> path = hitPath(event.x(), event.y());
        if (path.isEmpty()) {
            focus.clearFocus();
            pressed = null;
            pressedPath = List.of();
            return false;
        }
        pressed = path.get(path.size() - 1);
        pressedPath = path;
        focusFromPath(path);
        return bubble(path, event);
    }

    private boolean dispatchMouseReleased(InputEvent.MouseReleased event) {
        if (pressed == null) {
            return false;
        }
        boolean consumed = bubble(pressedPath, event);
        pressed = null;
        pressedPath = List.of();
        return consumed;
    }

    private boolean dispatchMouseDragged(InputEvent.MouseDragged event) {
        if (pressed == null) {
            return false;
        }
        return bubble(pressedPath, event);
    }

    private boolean dispatchMouseScrolled(InputEvent.MouseScrolled event) {
        return bubble(hitPath(event.x(), event.y()), event);
    }

    private boolean dispatchKeyPressed(InputEvent.KeyPressed event) {
        boolean consumed = dispatchToFocused(event);
        if (consumed) {
            return true;
        }
        if (event.keyCode() == Keys.TAB) {
            return (event.modifiers() & Keys.MOD_SHIFT) != 0 ? focus.focusPrevious() : focus.focusNext();
        }
        return false;
    }

    private boolean dispatchKeyReleased(InputEvent.KeyReleased event) {
        return dispatchToFocused(event);
    }

    private boolean dispatchCharTyped(InputEvent.CharTyped event) {
        return dispatchToFocused(event);
    }

    private boolean dispatchToFocused(InputEvent event) {
        Widget target = focus.focusedWidget().orElse(root);
        return target != null && target.onInput(event);
    }

    private void focusFromPath(List<Widget> path) {
        for (int i = path.size() - 1; i >= 0; i--) {
            Widget widget = path.get(i);
            if (widget.isFocusable()) {
                focus.requestFocus(widget);
                return;
            }
        }
        focus.clearFocus();
    }

    private void updateHover(Widget target) {
        if (hovered == target) {
            return;
        }
        Widget previous = hovered;
        hovered = target;
        if (previous != null) {
            previous.onHoverChanged(false);
        }
        if (target != null) {
            target.onHoverChanged(true);
        }
    }

    private List<Widget> hitPath(double x, double y) {
        List<Widget> path = new ArrayList<>();
        collectHit(root, x, y, UNBOUNDED_CLIP, path);
        return path;
    }

    private static boolean collectHit(Widget widget, double x, double y, Rect clip, List<Widget> path) {
        if (!widget.bounds().contains(x, y)) {
            return false;
        }
        path.add(widget);
        Rect childClip = widget.clipsChildren() ? clip.intersect(widget.bounds()) : clip;
        if (childClip.contains(x, y)) {
            List<Widget> children = widget.children();
            for (int i = children.size() - 1; i >= 0; i--) {
                if (collectHit(children.get(i), x, y, childClip, path)) {
                    return true;
                }
            }
        }
        return true;
    }

    private static List<Widget> collectFocusable(Widget widget) {
        List<Widget> result = new ArrayList<>();
        collectFocusable(widget, result);
        return result;
    }

    private static void collectFocusable(Widget widget, List<Widget> result) {
        if (widget.isFocusable()) {
            result.add(widget);
        }
        for (Widget child : widget.children()) {
            collectFocusable(child, result);
        }
    }

    private static boolean bubble(List<Widget> path, InputEvent event) {
        for (int i = path.size() - 1; i >= 0; i--) {
            if (path.get(i).onInput(event)) {
                return true;
            }
        }
        return false;
    }
}
