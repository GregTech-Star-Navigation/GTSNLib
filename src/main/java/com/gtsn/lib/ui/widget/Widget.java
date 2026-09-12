package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.layout.LayoutNode;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.render.RenderContext;

import java.util.List;

/**
 * 控件树契约：布局参与（{@link #node()}）、子节点、渲染、输入处理。
 *
 * <p>本接口不依赖任何 Minecraft 类型，可在无 MC 环境（单测 / GameTest 服务端）中构建与驱动；
 * 具体渲染由 {@link RenderContext} 抽象承接。</p>
 *
 * <p>输入处理约定：事件从命中目标向上冒泡，返回 {@code true} 表示消费并终止传播；
 * 键盘/字符事件先交给焦点控件，无焦点时交给根控件。</p>
 */
public interface Widget {

    /** 该控件的布局节点；控件构造时创建，子节点通过容器 API 挂接到父节点上。 */
    LayoutNode node();

    /** 子控件（渲染顺序即列表顺序，后添加者在上层）；叶控件返回空列表。 */
    default List<Widget> children() {
        return List.of();
    }

    /** 绘制自身与子节点。 */
    default void render(RenderContext context) {
    }

    /** 处理输入事件；返回 {@code true} 表示已消费。 */
    default boolean onInput(InputEvent event) {
        return false;
    }

    /** 是否可接收焦点（键盘/字符输入）。 */
    default boolean isFocusable() {
        return false;
    }

    /** 是否裁剪子节点渲染与命中测试（如滚动视口）。 */
    default boolean clipsChildren() {
        return false;
    }

    /** 布局完成后的绝对包围盒快捷访问。 */
    default Rect bounds() {
        return node().bounds();
    }

    /** 焦点状态变化回调。 */
    default void onFocusChanged(boolean focused) {
    }

    /** 悬停状态变化回调。 */
    default void onHoverChanged(boolean hovered) {
    }
}
