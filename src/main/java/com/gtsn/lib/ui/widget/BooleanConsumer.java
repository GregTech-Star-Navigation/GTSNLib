package com.gtsn.lib.ui.widget;

/**
 * 布尔值消费者：开关类控件状态变化回调，避免 {@code Consumer<Boolean>} 的装箱歧义。
 */
@FunctionalInterface
public interface BooleanConsumer {

    void accept(boolean value);
}
