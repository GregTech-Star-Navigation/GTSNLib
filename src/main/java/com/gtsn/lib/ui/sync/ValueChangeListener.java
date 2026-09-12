package com.gtsn.lib.ui.sync;

/**
 * 同步值变化监听器：{@link MenuSync#refresh()} 检测到槽位原始值变化时回调。
 *
 * <p>监听器不得抛异常；抛出的运行时异常会被记录并跳过该监听器，不阻断其余监听器与同步流程。</p>
 *
 * @param <T> 槽位类型化值
 */
@FunctionalInterface
public interface ValueChangeListener<T> {

    /** 值发生变化时回调（旧值 → 新值，均为解码后的类型化值）。 */
    void onChanged(T oldValue, T newValue);
}
