package com.gtsn.lib.ui.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 同步状态：把 {@link SyncLayout} 绑定到某个 {@link IntStore} 后端，提供类型化读写与变更通知。
 *
 * <p>服务端：构造菜单时 {@link #attach} + 写入（{@link #set}），原版 {@code AbstractContainerMenu}
 * 每次 tick 的 {@code broadcastChanges()} 把变化过的 int 数据槽推给客户端；
 * 客户端：同一个菜单构造路径创建客户端后端，收到的数据经数据槽写入后端；
 * 界面在 tick 中调用 {@link #refresh()} 检测变化并触发监听器（绑定层据此更新控件）。</p>
 *
 * <p>本类纯 Java（不依赖 Minecraft），可在单测中以假后端驱动。</p>
 */
public final class MenuSync {

    private static final Logger LOGGER = LoggerFactory.getLogger(MenuSync.class);

    private final SyncLayout layout;
    private final IntStore store;
    private final int[] lastSeen;
    private final List<List<ListenerEntry<?>>> listeners;

    private MenuSync(SyncLayout layout, IntStore store) {
        this.layout = layout;
        this.store = store;
        this.lastSeen = new int[layout.slotCount()];
        this.listeners = new ArrayList<>(layout.slotCount());
        for (int i = 0; i < layout.slotCount(); i++) {
            this.listeners.add(new ArrayList<>());
            int defaultRaw = layout.slotAt(i).defaultRaw();
            store.set(i, defaultRaw);
            lastSeen[i] = defaultRaw;
        }
    }

    /**
     * 绑定布局与后端：校验后端容量，写入全部槽位默认值（服务端后续写入 / 客户端收包覆盖），并记录快照。
     */
    public static MenuSync attach(SyncLayout layout, IntStore store) {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(store, "store");
        if (store.size() < layout.slotCount()) {
            throw new IllegalArgumentException("store too small: " + store.size() + " < " + layout.slotCount());
        }
        return new MenuSync(layout, store);
    }

    /** 布局。 */
    public SyncLayout layout() {
        return layout;
    }

    /** 读取槽位当前值（客户端读取已同步值；服务端读取已写入值）。 */
    public <T> T get(SyncSlot<T> slot) {
        int index = requireIndex(slot);
        return slot.codec().decode(store.get(index));
    }

    /** 写入槽位值（服务端权威写；经编解码器钳制/校验）。 */
    public <T> void set(SyncSlot<T> slot, T value) {
        Objects.requireNonNull(value, "value");
        int index = requireIndex(slot);
        store.set(index, slot.codec().encode(value));
    }

    /**
     * 检测后端原始值变化并通知监听器（客户端在 tick 中调用；服务端也可用于响应直写）。
     *
     * @return 发生变化的槽位数量
     */
    public int refresh() {
        int changes = 0;
        for (int index = 0; index < lastSeen.length; index++) {
            int raw = store.get(index);
            if (raw == lastSeen[index]) {
                continue;
            }
            int previous = lastSeen[index];
            lastSeen[index] = raw;
            changes++;
            notifyListeners(index, previous, raw);
        }
        return changes;
    }

    /** 订阅槽位值变化；返回句柄用于取消。 */
    public <T> SyncSubscription onChange(SyncSlot<T> slot, ValueChangeListener<T> listener) {
        Objects.requireNonNull(listener, "listener");
        int index = requireIndex(slot);
        ListenerEntry<T> entry = new ListenerEntry<>(listener);
        listeners.get(index).add(entry);
        return new SyncSubscription() {
            @Override
            public void cancel() {
                if (entry.active) {
                    entry.active = false;
                    listeners.get(index).remove(entry);
                }
            }

            @Override
            public boolean active() {
                return entry.active;
            }
        };
    }

    private int requireIndex(SyncSlot<?> slot) {
        Objects.requireNonNull(slot, "slot");
        int index = slot.index();
        if (index < 0 || index >= layout.slotCount() || layout.slotAt(index) != slot) {
            throw new IllegalArgumentException("slot " + slot + " does not belong to this layout");
        }
        return index;
    }

    @SuppressWarnings("unchecked")
    private void notifyListeners(int index, int previousRaw, int raw) {
        List<ListenerEntry<?>> entries = listeners.get(index);
        if (entries.isEmpty()) {
            return;
        }
        SyncSlot<?> slot = layout.slotAt(index);
        SyncCodec<Object> codec = (SyncCodec<Object>) slot.codec();
        Object oldValue = codec.decode(previousRaw);
        Object newValue = codec.decode(raw);
        // 快照遍历：监听器取消订阅不影响本次通知的其余监听器。
        for (ListenerEntry<?> entry : List.copyOf(entries)) {
            if (!entry.active) {
                continue;
            }
            try {
                ((ListenerEntry<Object>) entry).deliver(oldValue, newValue);
            } catch (RuntimeException exception) {
                LOGGER.error("[GTSNLib] sync listener failed for slot '{}'", slot.name(), exception);
            }
        }
    }

    /** 单个监听器订阅项。 */
    private static final class ListenerEntry<T> {

        private final ValueChangeListener<T> listener;
        private boolean active = true;

        private ListenerEntry(ValueChangeListener<T> listener) {
            this.listener = listener;
        }

        private void deliver(T oldValue, T newValue) {
            listener.onChanged(oldValue, newValue);
        }
    }
}
