package com.gtsn.lib.ui;

import com.gtsn.lib.ui.widget.TextMetrics;

/**
 * 测试字体度量：宽度按 6px/字符、行高 9px，与真实字体无关但固定可预期。
 */
public final class TestTextMetrics implements TextMetrics {

    public static final TestTextMetrics INSTANCE = new TestTextMetrics();

    private TestTextMetrics() {
    }

    @Override
    public int width(String text) {
        return text.length() * 6;
    }

    @Override
    public int lineHeight() {
        return 9;
    }
}
