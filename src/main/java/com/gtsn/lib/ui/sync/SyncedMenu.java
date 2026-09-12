package com.gtsn.lib.ui.sync;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import javax.annotation.Nullable;

/**
 * 带数据同步的容器菜单基类：构造期按 {@link SyncLayout} 创建后端并注册为原版数据槽
 * （{@code addDataSlots}），子类经 {@link #sync()} 读写同步值。
 *
 * <p>同一构造器在服务端与客户端各执行一次（客户端经 {@code IContainerFactory} 创建），
 * 两端共享同一布局定义；服务端写经原版数据槽机制自动同步到客户端。</p>
 *
 * <p>公共侧类；客户端界面实现由使用方在客户端注册（如 {@code MenuScreens.register}）。</p>
 */
public abstract class SyncedMenu extends AbstractContainerMenu {

    private final MenuSyncData syncData;
    private final MenuSync sync;

    protected SyncedMenu(@Nullable MenuType<?> menuType, int containerId, SyncLayout layout) {
        super(menuType, containerId);
        this.syncData = new MenuSyncData(layout.slotCount());
        this.addDataSlots(this.syncData);
        this.sync = MenuSync.attach(layout, syncData);
    }

    /** 类型化同步状态（服务端写 / 客户端读）。 */
    public final MenuSync sync() {
        return sync;
    }

    /** 数据槽后端（诊断 / 测试用）。 */
    public final MenuSyncData syncData() {
        return syncData;
    }

    /**
     * 立即把已写入的变化推送给客户端（服务端调用）。
     *
     * <p>等价于原版下一次 tick 的 {@code broadcastChanges()}；用于交互后需要即时反馈的场景。</p>
     */
    public final void flushSync() {
        broadcastChanges();
    }
}
