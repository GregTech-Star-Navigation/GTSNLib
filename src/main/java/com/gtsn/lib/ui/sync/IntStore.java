package com.gtsn.lib.ui.sync;

/**
 * 同步值后端的最小抽象：以 int 槽位数组读写。
 *
 * <p>纯 Java 接口（不依赖 Minecraft），使同步核心逻辑可在无 MC 环境（单测 / GameTest）中以
 * 假后端驱动；游戏内后端为 {@link MenuSyncData}（包装原版 {@code SimpleContainerData}，
 * 经 {@code AbstractContainerMenu.addDataSlots} 接入容器菜单的数据槽同步）。</p>
 */
public interface IntStore {

    /** 读取槽位原始值。 */
    int get(int index);

    /** 写入槽位原始值。 */
    void set(int index, int value);

    /** 槽位数量。 */
    int size();
}
