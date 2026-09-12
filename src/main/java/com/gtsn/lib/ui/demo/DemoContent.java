package com.gtsn.lib.ui.demo;

import com.gtsn.lib.ui.layout.Anchor;
import com.gtsn.lib.ui.layout.CrossAxisAlign;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.MainAxisAlign;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.widget.BoxWidget;
import com.gtsn.lib.ui.widget.ButtonWidget;
import com.gtsn.lib.ui.widget.ClipWidget;
import com.gtsn.lib.ui.widget.Stack;
import com.gtsn.lib.ui.widget.TextMetrics;
import com.gtsn.lib.ui.widget.TextWidget;
import com.gtsn.lib.ui.widget.Widget;

import java.util.Objects;

/**
 * 开发测试界面的内容装配：布局演示（栈/权重/对齐/锚点/裁剪）、按钮点击、键盘输入演示。
 *
 * <p>纯控件树，不依赖 Minecraft；客户端窗口与 GameTest 共用同一份装配保证行为一致。</p>
 */
public record DemoContent(
        Widget root,
        DemoState state,
        ButtonWidget clickButton,
        ButtonWidget toggleButton,
        ButtonWidget clearButton,
        KeypadWidget keypad,
        TextWidget clickStatus,
        TextWidget toggleStatus,
        ClipWidget clipShowcase,
        Widget badge) {

    public static DemoContent build(TextMetrics metrics) {
        return build(new DemoState(), metrics);
    }

    public static DemoContent build(DemoState state, TextMetrics metrics) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(metrics, "metrics");

        Stack root = Stack.vertical().padding(Insets.all(10)).gap(6);
        root.add(new TextWidget("GTSN UI Kernel — 开发测试界面", metrics).shadow(true));
        root.add(new TextWidget("布局 / 渲染 / 输入 / 焦点 内核演示", metrics).color(0xFF9FB8D0));
        TextWidget toggleStatus = root.add(new TextWidget("状态: A", metrics).color(0xFFB0B0B0));
        TextWidget clickStatus = new TextWidget("点击次数: 0", metrics).color(0xFFB0B0B0);
        KeypadWidget keypad = new KeypadWidget(state);

        Stack buttonRow = root.add(Stack.horizontal().gap(6));
        ButtonWidget clickButton = buttonRow.add(new ButtonWidget("点击 +1", metrics, () -> {
            state.incrementClicks();
            clickStatus.text("点击次数: " + state.clicks());
        }));
        ButtonWidget toggleButton = buttonRow.add(new ButtonWidget("切换状态", metrics, () -> {
            state.toggle();
            toggleStatus.text("状态: " + (state.toggled() ? "B" : "A"))
                    .color(state.toggled() ? 0xFF7FD08A : 0xFFB0B0B0);
        }));
        ButtonWidget clearButton = buttonRow.add(new ButtonWidget("清空输入", metrics, () -> {
            state.clearTyped();
            state.lastKey("(none)");
        }));

        root.add(clickStatus);

        // 裁剪/权重/间距演示：行内第三块按权重吃掉剩余宽度，行下方一块宽度 500 的盒子会被 scissor 裁掉。
        ClipWidget clipShowcase = root.add(
                new ClipWidget().size(Sizing.fill(), Sizing.fixed(70)).padding(Insets.all(6)));
        Stack showcaseColumn = clipShowcase.add(Stack.vertical().gap(4));
        Stack boxesRow = showcaseColumn.add(Stack.horizontal().gap(6));
        boxesRow.add(new BoxWidget().fixedSize(46, 20).fill(0xFF4878A8).border(0xFF9CC4E4, 1));
        boxesRow.add(new BoxWidget().size(Sizing.fill(), Sizing.fixed(20)).weight(1)
                .fill(0xFF507850).border(0xFF9CC4E4, 1));
        boxesRow.add(new BoxWidget().fixedSize(46, 20).fill(0xFFB08A48).border(0xFF9CC4E4, 1));
        showcaseColumn.add(new BoxWidget().fixedSize(500, 30).fill(0xFF803030));

        root.add(keypad);

        // 锚定演示：绝对定位徽标固定在根内容区右下角。
        Stack badge = root.add(Stack.vertical().fixedSize(108, 16)
                .align(MainAxisAlign.CENTER, CrossAxisAlign.CENTER)
                .absolute(Anchor.BOTTOM_RIGHT, -4, -4));
        badge.add(new TextWidget("kernel phase 1", metrics).color(0xFF8FB0C8));

        return new DemoContent(root, state, clickButton, toggleButton, clearButton, keypad,
                clickStatus, toggleStatus, clipShowcase, badge);
    }
}
