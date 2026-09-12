package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Sizing;

/**
 * 占位控件：只参与布局、不产生任何绘制；用于推挤其它控件或吃掉剩余空间。
 *
 * <pre>{@code
 * row.add(new SpacerWidget().fixedSize(8, 0));   // 固定间距
 * row.add(SpacerWidget.weight(1));               // 弹性撑开
 * }</pre>
 */
public final class SpacerWidget extends AbstractWidget {

    public SpacerWidget() {
    }

    /** 固定像素占位。 */
    public static SpacerWidget fixed(int width, int height) {
        return new SpacerWidget().fixedSize(width, height);
    }

    public SpacerWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public SpacerWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public SpacerWidget weight(float weight) {
        node().params().weight(weight);
        return this;
    }

    public SpacerWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }
}
