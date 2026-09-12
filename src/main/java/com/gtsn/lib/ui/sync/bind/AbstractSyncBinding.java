package com.gtsn.lib.ui.sync.bind;

import com.gtsn.lib.ui.sync.MenuSync;
import com.gtsn.lib.ui.sync.SyncSlot;
import com.gtsn.lib.ui.sync.SyncSubscription;

import java.util.Objects;

/**
 * 同步值绑定基类：订阅槽位变化，值变化时把解码后的类型化值交给 {@link #apply(T)}。
 *
 * <p>构造期不应用初值（避免在子类字段初始化前调用 {@link #apply(T)}）；由
 * {@link SyncBindings} 工厂或 {@link SyncBindingGroup#add} 在构造完成后调用 {@link #refresh()} 完成
 * 初始应用。自定义子类必须自行完成一次 {@link #refresh()}。</p>
 *
 * <p>重复应用去重：值未变化时不调用 {@link #apply(T)}。</p>
 */
public abstract class AbstractSyncBinding<T> implements SyncBinding {

    private final MenuSync sync;
    private final SyncSlot<T> slot;
    private final SyncSubscription subscription;
    private T lastApplied;
    private boolean applied;

    protected AbstractSyncBinding(MenuSync sync, SyncSlot<T> slot) {
        this.sync = Objects.requireNonNull(sync, "sync");
        this.slot = Objects.requireNonNull(slot, "slot");
        this.subscription = sync.onChange(slot, this::onChanged);
    }

    /** 绑定的槽位。 */
    public final SyncSlot<T> slot() {
        return slot;
    }

    @Override
    public final void refresh() {
        applyIfChanged(sync.get(slot));
    }

    /** 把类型化值应用到目标控件。 */
    protected abstract void apply(T value);

    @Override
    public void close() {
        subscription.cancel();
    }

    private void onChanged(T oldValue, T newValue) {
        applyIfChanged(newValue);
    }

    private void applyIfChanged(T value) {
        if (applied && Objects.equals(lastApplied, value)) {
            return;
        }
        applied = true;
        lastApplied = value;
        apply(value);
    }
}
