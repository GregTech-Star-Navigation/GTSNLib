package com.gtsn.lib.ui.widget;

/**
 * 字体度量抽象：文本控件的固有尺寸来源，由客户端以真实字体实现，测试可注入固定值。
 */
public interface TextMetrics {

    int width(String text);

    int lineHeight();
}
