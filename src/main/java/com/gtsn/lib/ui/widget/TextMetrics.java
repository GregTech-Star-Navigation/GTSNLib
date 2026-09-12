package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.theme.FontId;

/**
 * 字体度量抽象：文本控件的固有尺寸来源，由客户端以真实字体实现，测试可注入固定值。
 *
 * <p>字体感知（#23）：{@link #width(String, FontId)} 度量指定字体的文本宽度，默认忽略字体
 * （无字体能力的实现回退 {@link #width(String)}）。客户端实现按主题字体 / 逐控件覆盖度量，
 * 保证布局尺寸与渲染宽度一致。</p>
 */
public interface TextMetrics {

    int width(String text);

    int lineHeight();

    /** 指定字体下的文本宽度；默认与 {@link #width(String)} 相同（忽略字体）。 */
    default int width(String text, FontId font) {
        return width(text);
    }
}
