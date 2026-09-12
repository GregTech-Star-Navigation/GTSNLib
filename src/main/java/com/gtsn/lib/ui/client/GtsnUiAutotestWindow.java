package com.gtsn.lib.ui.client;

import com.mojang.blaze3d.platform.Window;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

/**
 * 自动测试的窗口准备（开发专用）：把窗口恢复显示并固定为 1280x720 + GUI 缩放 2，保证截图清晰、
 * 合成输入坐标有效。
 *
 * <p>未聚焦 / 最小化的 GLFW 窗口不会应用尺寸变化，且帧缓冲尺寸会退化为 1x1（渲染目标随之退化），
 * 因此先 {@code glfwShowWindow}/{@code glfwRestoreWindow} 再设置尺寸；截图前可再次调用
 * {@link #ensureSized} 兜底。仅客户端加载。</p>
 */
final class GtsnUiAutotestWindow {

    private static final int WIDTH = 1280;
    private static final int HEIGHT = 720;
    private static final int MIN_USABLE_WIDTH = 320;
    private static final int MIN_USABLE_HEIGHT = 240;

    private static final Logger LOGGER = LogUtils.getLogger();

    private GtsnUiAutotestWindow() {
    }

    /** 恢复窗口显示并固定尺寸（{@code label} 用于日志区分自动测试类型）。 */
    static void prepare(Minecraft minecraft, String label) {
        Window window = minecraft.getWindow();
        GLFW.glfwShowWindow(window.getWindow());
        GLFW.glfwRestoreWindow(window.getWindow());
        window.setWindowed(WIDTH, HEIGHT);
        minecraft.options.guiScale().set(2);
        minecraft.resizeDisplay();
        LOGGER.info("[GTSNLib] {} window prepared: {}x{} guiScale=2",
                label, window.getWidth(), window.getHeight());
    }

    /** 截图前确保窗口可用尺寸；窗口被最小化 / 桌面会话变化时重新恢复并应用尺寸。 */
    static void ensureSized(Minecraft minecraft, String label) {
        Window window = minecraft.getWindow();
        if (window.getWidth() < MIN_USABLE_WIDTH || window.getHeight() < MIN_USABLE_HEIGHT) {
            GLFW.glfwShowWindow(window.getWindow());
            GLFW.glfwRestoreWindow(window.getWindow());
            window.setWindowed(WIDTH, HEIGHT);
            minecraft.resizeDisplay();
            LOGGER.info("[GTSNLib] {} re-applied window size: {}x{}",
                    label, window.getWidth(), window.getHeight());
        }
    }
}
