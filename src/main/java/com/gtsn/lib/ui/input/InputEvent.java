package com.gtsn.lib.ui.input;

/**
 * UI 输入事件模型：鼠标移动/按键/拖拽/滚轮、键盘按键/字符。
 *
 * <p>坐标单位为 GUI 像素；键盘码与修饰键为 GLFW 常量值（见 {@link Keys}），
 * 但模型本身不依赖 Minecraft 或 GLFW。</p>
 */
public sealed interface InputEvent {

    record MouseMoved(double x, double y) implements InputEvent {
    }

    record MousePressed(double x, double y, int button) implements InputEvent {
    }

    record MouseReleased(double x, double y, int button) implements InputEvent {
    }

    record MouseDragged(double x, double y, int button, double deltaX, double deltaY) implements InputEvent {
    }

    record MouseScrolled(double x, double y, double scrollX, double scrollY) implements InputEvent {
    }

    record KeyPressed(int keyCode, int scanCode, int modifiers) implements InputEvent {
    }

    record KeyReleased(int keyCode, int scanCode, int modifiers) implements InputEvent {
    }

    record CharTyped(char codePoint, int modifiers) implements InputEvent {
    }
}
