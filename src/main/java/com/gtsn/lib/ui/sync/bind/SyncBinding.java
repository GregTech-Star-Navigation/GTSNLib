package com.gtsn.lib.ui.sync.bind;

/**
 * 单个同步值 → 控件的绑定：初始应用当前值，随后在宿主 tick 中随同步变化刷新。
 *
 * <p>绑定的生命周期由创建它的屏幕/宿主管理；{@link #close()} 必须解绑监听，避免屏幕关闭后
 * 仍收到回调。</p>
 */
public interface SyncBinding extends AutoCloseable {

    /** 把当前同步值应用到目标控件（值未变化时不重复应用）。 */
    void refresh();

    /** 解绑并释放资源（幂等）。 */
    @Override
    void close();
}
