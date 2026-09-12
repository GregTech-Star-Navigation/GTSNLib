package com.gtsn.lib.ui.layout;

/**
 * 叶节点的固有内容测量：在给定约束下返回内容自身尺寸（不含 padding/margin）。
 * 例如文本控件可用字体度量实现该接口。
 */
@FunctionalInterface
public interface ContentMeasurer {

    Size measure(MeasureSpec widthSpec, MeasureSpec heightSpec);
}
