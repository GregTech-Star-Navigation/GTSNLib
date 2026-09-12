package com.gtsn.lib.ui.widget;

/**
 * 固定宽度字体度量（6px/字符、行高 9px）：供无渲染环境的布局（GameTest / 单元测试）与控制台场景使用，
 * 真实客户端应改用 Minecraft 字体实现。
 */
public enum PlainTextMetrics implements TextMetrics {
    INSTANCE;

    @Override
    public int width(String text) {
        return text.length() * 6;
    }

    @Override
    public int lineHeight() {
        return 9;
    }
}
