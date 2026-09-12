package com.gtsn.lib.ui.sync.bind;

import com.gtsn.lib.ui.sync.MenuSync;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 绑定组：屏幕上全部同步绑定的集合，{@link #refresh()} 由屏幕 tick 统一驱动
 * （先检测数据槽变化并触发通知，再落实控件更新）。
 *
 * <p>屏幕关闭时必须 {@link #close()}，解绑全部监听。</p>
 */
public final class SyncBindingGroup implements AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(SyncBindingGroup.class);

    private final MenuSync sync;
    private final List<SyncBinding> bindings = new ArrayList<>();
    private boolean closed;

    private SyncBindingGroup(MenuSync sync) {
        this.sync = Objects.requireNonNull(sync, "sync");
    }

    public static SyncBindingGroup of(MenuSync sync) {
        return new SyncBindingGroup(sync);
    }

    /** 加入绑定并立即应用其当前值；重复加入同一绑定不会重复应用（绑定侧去重）。 */
    public <B extends SyncBinding> B add(B binding) {
        Objects.requireNonNull(binding, "binding");
        if (closed) {
            throw new IllegalStateException("binding group already closed");
        }
        bindings.add(binding);
        binding.refresh();
        return binding;
    }

    /** 检测并应用全部同步变化；返回发生变化的槽位数量。 */
    public int refresh() {
        return sync.refresh();
    }

    /** 已加入的绑定数量。 */
    public int size() {
        return bindings.size();
    }

    /** 解绑全部绑定（幂等）。单个绑定的解绑异常不影响其余绑定。 */
    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        for (SyncBinding binding : bindings) {
            try {
                binding.close();
            } catch (RuntimeException exception) {
                LOGGER.error("[GTSNLib] failed to close sync binding {}", binding, exception);
            }
        }
        bindings.clear();
    }
}
