package com.gtsn.lib.ui.input;

import com.gtsn.lib.ui.layout.LayoutEngine;
import com.gtsn.lib.ui.layout.Rect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 输入路由行为：命中测试（深度/顺序/裁剪）、冒泡派发、拖拽捕获、
 * 悬停跟踪、点击聚焦与 Tab 换焦、键盘事件路由。
 */
class InputRouterTest {

    private final List<String> log = new ArrayList<>();

    private static void layout(ProbeWidget root, int width, int height) {
        LayoutEngine.layout(root.node(), Rect.of(0, 0, width, height));
    }

    @Test
    void hitTestPrefersTopmostOverlappingChildAndFallsBackToParent() {
        ProbeWidget root = new ProbeWidget("root", log);
        ProbeWidget a = new ProbeWidget("a", log).fixed(40, 40);
        ProbeWidget b = new ProbeWidget("b", log).fixed(40, 40).absoluteTopLeft();
        root.add(a, b);
        layout(root, 100, 100);
        InputRouter router = new InputRouter(root);

        assertEquals(Optional.of(b), router.hitTest(10, 10), "后添加的重叠子节点在上层");
        assertEquals(Optional.of(root), router.hitTest(50, 50), "空白处命中父节点");
        assertEquals(Optional.empty(), router.hitTest(150, 150), "树外无命中");
    }

    @Test
    void clickBubblesToAncestorsUntilConsumed() {
        ProbeWidget root = new ProbeWidget("root", log).silent();
        ProbeWidget child = new ProbeWidget("child", log).fixed(40, 40).silent();
        root.add(child);
        layout(root, 100, 100);
        InputRouter router = new InputRouter(root);

        assertFalse(router.dispatch(new InputEvent.MousePressed(10, 10, 0)));
        assertEquals(List.of("child:press", "root:press"), log, "全部不消费时冒泡到根");

        log.clear();
        ProbeWidget consumingRoot = new ProbeWidget("root2", log).silent();
        ProbeWidget mid = new ProbeWidget("mid", log).fixed(60, 60);
        ProbeWidget deep = new ProbeWidget("deep", log).fixed(20, 20).silent();
        mid.add(deep);
        consumingRoot.add(mid);
        layout(consumingRoot, 100, 100);
        InputRouter router2 = new InputRouter(consumingRoot);

        assertTrue(router2.dispatch(new InputEvent.MousePressed(10, 10, 0)));
        assertEquals(List.of("deep:press", "mid:press"), log, "消费后不再向上冒泡");
    }

    @Test
    void mouseMoveTracksHoverState() {
        ProbeWidget root = new ProbeWidget("root", log).silent();
        ProbeWidget a = new ProbeWidget("a", log).fixed(40, 40).silent();
        root.add(a);
        layout(root, 100, 100);
        InputRouter router = new InputRouter(root);

        router.dispatch(new InputEvent.MouseMoved(10, 10));
        assertEquals(Optional.of(a), router.hovered());
        assertTrue(log.contains("a:hover=true"), "进入控件触发悬停回调: " + log);

        log.clear();
        router.dispatch(new InputEvent.MouseMoved(150, 150));
        assertEquals(Optional.empty(), router.hovered());
        assertTrue(log.contains("a:hover=false"), "移出控件触发悬停回调: " + log);

        log.clear();
        router.dispatch(new InputEvent.MouseMoved(10, 10));
        assertEquals(Optional.of(a), router.hovered());
        assertTrue(log.contains("a:hover=true"));
    }

    @Test
    void pressCapturesDragAndReleaseEvenOutsideBounds() {
        ProbeWidget root = new ProbeWidget("root", log).silent();
        ProbeWidget a = new ProbeWidget("a", log).fixed(40, 40);
        root.add(a);
        layout(root, 100, 100);
        InputRouter router = new InputRouter(root);

        assertTrue(router.dispatch(new InputEvent.MousePressed(10, 10, 0)));
        assertTrue(router.pressedWidget().isPresent());

        log.clear();
        router.dispatch(new InputEvent.MouseDragged(500, 500, 0, 490, 490));
        assertEquals(List.of("a:drag"), log, "拖拽派发给按下时捕获的控件");

        log.clear();
        assertTrue(router.dispatch(new InputEvent.MouseReleased(500, 500, 0)));
        assertEquals(List.of("a:release"), log);
        assertTrue(router.pressedWidget().isEmpty(), "释放后清除捕获");

        log.clear();
        router.dispatch(new InputEvent.MouseDragged(500, 500, 0, 0, 0));
        assertTrue(log.isEmpty(), "无捕获时拖拽不再派发");
    }

    @Test
    void scrollGoesToWidgetUnderPointer() {
        ProbeWidget root = new ProbeWidget("root", log).silent();
        ProbeWidget a = new ProbeWidget("a", log).fixed(40, 40);
        root.add(a);
        layout(root, 100, 100);
        InputRouter router = new InputRouter(root);

        boolean consumed = router.dispatch(new InputEvent.MouseScrolled(10, 10, 0, 1));

        assertTrue(consumed);
        assertEquals(List.of("a:scroll"), log);
    }

    @Test
    void pressFocusesNearestFocusableAncestorAndClearsOnEmptyArea() {
        ProbeWidget root = new ProbeWidget("root", log).silent();
        ProbeWidget a = new ProbeWidget("a", log).fixed(40, 40).focusable().silent();
        ProbeWidget leaf = new ProbeWidget("leaf", log).fixed(10, 10).silent();
        a.add(leaf);
        root.add(a);
        layout(root, 100, 100);
        InputRouter router = new InputRouter(root);

        router.dispatch(new InputEvent.MousePressed(5, 5, 0));
        assertTrue(router.focus().isFocused(a), "命中非焦点叶节点时聚焦最近可聚焦祖先");
        assertTrue(log.contains("a:focus=true"));

        log.clear();
        router.dispatch(new InputEvent.MousePressed(80, 80, 0));
        assertTrue(router.focus().focusedWidget().isEmpty(), "点击无可聚焦目标时清除焦点");
        assertTrue(log.contains("a:focus=false"));
    }

    @Test
    void tabCyclesFocusBothDirectionsAndWidgetCanConsumeTab() {
        ProbeWidget root = new ProbeWidget("root", log).silent();
        ProbeWidget a = new ProbeWidget("a", log).fixed(20, 20).focusable().silent();
        ProbeWidget b = new ProbeWidget("b", log).fixed(20, 20).focusable().silent();
        ProbeWidget c = new ProbeWidget("c", log).fixed(20, 20).focusable().silent();
        root.add(a, b, c);
        layout(root, 100, 100);
        InputRouter router = new InputRouter(root);

        router.dispatch(new InputEvent.KeyPressed(Keys.TAB, 0, 0));
        assertTrue(router.focus().isFocused(a), "首次 Tab 聚焦顺序中的第一个");
        router.dispatch(new InputEvent.KeyPressed(Keys.TAB, 0, 0));
        assertTrue(router.focus().isFocused(b));
        router.dispatch(new InputEvent.KeyPressed(Keys.TAB, 0, 0));
        assertTrue(router.focus().isFocused(c));
        router.dispatch(new InputEvent.KeyPressed(Keys.TAB, 0, 0));
        assertTrue(router.focus().isFocused(a), "顺序末尾回绕到第一个");
        router.dispatch(new InputEvent.KeyPressed(Keys.TAB, 0, Keys.MOD_SHIFT));
        assertTrue(router.focus().isFocused(c), "Shift+Tab 反向回绕");

        ProbeWidget consumer = new ProbeWidget("consumer", log).fixed(20, 20).focusable().consume(true);
        router.focus().requestFocus(consumer);
        log.clear();
        assertTrue(router.dispatch(new InputEvent.KeyPressed(Keys.TAB, 0, 0)));
        assertTrue(router.focus().isFocused(consumer), "焦点控件消费 Tab 时不切换");
        assertEquals(List.of("consumer:key"), log);
    }

    @Test
    void keyAndCharEventsGoToFocusedWidgetOrRootWithoutFocus() {
        ProbeWidget root = new ProbeWidget("root", log);
        ProbeWidget a = new ProbeWidget("a", log).fixed(20, 20).focusable();
        root.add(a);
        layout(root, 100, 100);
        InputRouter router = new InputRouter(root);

        assertTrue(router.dispatch(new InputEvent.KeyPressed('K', 0, 0)));
        assertEquals(List.of("root:key"), log, "无焦点时键盘事件交给根控件");

        log.clear();
        router.focus().requestFocus(a);
        router.dispatch(new InputEvent.KeyPressed('K', 0, 0));
        router.dispatch(new InputEvent.KeyReleased('K', 0, 0));
        router.dispatch(new InputEvent.CharTyped('x', 0));

        assertEquals(List.of("a:focus=true", "a:key", "a:keyUp", "a:char"), log);
    }

    @Test
    void clippedChildrenAreNotHittableOutsideClip() {
        ProbeWidget root = new ProbeWidget("root", log).silent();
        ProbeWidget viewport = new ProbeWidget("viewport", log).fixed(20, 20).clipping();
        ProbeWidget oversized = new ProbeWidget("oversized", log).fixed(100, 100).silent();
        viewport.add(oversized);
        root.add(viewport);
        layout(root, 100, 100);
        InputRouter router = new InputRouter(root);

        assertEquals(Optional.of(oversized), router.hitTest(10, 10), "裁剪区内仍命中溢出子节点");
        assertEquals(Optional.of(root), router.hitTest(50, 50), "裁剪区外不可命中被裁剪子树");
    }
}
