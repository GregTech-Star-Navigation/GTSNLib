package com.gtsn.lib.ui.sync;

/**
 * 单个数据槽的类型编解码器：把类型化值映射到容器菜单可同步的 int（原版数据槽只承载 int）。
 *
 * <p>约定：</p>
 * <ul>
 *   <li>{@link #encode} 面对非法值（如非有限浮点）显式拒绝，面对可恢复的越界值按编解码器规则钳制；</li>
 *   <li>{@link #decode} 面对任意（含损坏）原始值必须返回一个合法值，不得抛异常——客户端可能收到缺失或
 *       前向不兼容的数据，解码层是最后的兜底。</li>
 * </ul>
 */
public interface SyncCodec<T> {

    /** 类型化值 → 原始 int。 */
    int encode(T value);

    /** 原始 int → 类型化值（任意输入都必须得到合法值）。 */
    T decode(int raw);
}
