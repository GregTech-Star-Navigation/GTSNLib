package com.gtsn.lib.ui.sync;

import net.minecraft.world.inventory.SimpleContainerData;

/**
 * 容器菜单数据槽后端：原版 {@link SimpleContainerData} 的 {@link IntStore} 适配。
 *
 * <p>经 {@code AbstractContainerMenu.addDataSlots(data)} 注册后：服务端对后端的写入由原版同步机制
 * 推送给客户端（{@code broadcastChanges()} → {@code ClientboundContainerSetDataPacket}），
 * 客户端收到包后经 {@code AbstractContainerMenu.setData} 写回后端，{@link MenuSync#refresh()} 即可读到。</p>
 *
 * <p>公共侧类（仅引用原版公共类型），专职服务端可加载。</p>
 */
public final class MenuSyncData extends SimpleContainerData implements IntStore {

    public MenuSyncData(int size) {
        super(size);
    }

    @Override
    public int size() {
        return getCount();
    }
}
