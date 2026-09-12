package com.gtsn.lib.ui.sync;

/**
 * 监听器订阅句柄：取消后不再接收回调。重复取消是幂等的。
 */
public interface SyncSubscription extends AutoCloseable {

    /** 取消订阅。 */
    void cancel();

    /** 订阅是否仍生效。 */
    boolean active();

    @Override
    default void close() {
        cancel();
    }
}
