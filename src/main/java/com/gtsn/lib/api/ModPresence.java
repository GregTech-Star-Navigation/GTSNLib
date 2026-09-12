package com.gtsn.lib.api;

/**
 * 可注入的 mod 存在性判定。
 *
 * <p>抽成函数式接口是为了让注册表在单元测试中注入假实现，无需启动 Forge。生产环境使用
 * {@code com.gtsn.lib.core.ForgeModPresence}，同时覆盖加载早期（{@code FMLLoader.getLoadingModList()}）
 * 与加载完成后（{@code ModList.get()}）两个时机。</p>
 */
@FunctionalInterface
public interface ModPresence {

    /**
     * @param modId 目标 mod 的 ModID
     * @return 目标 mod 当前是否在场
     */
    boolean isLoaded(String modId);
}
