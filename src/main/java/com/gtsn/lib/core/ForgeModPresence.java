package com.gtsn.lib.core;

import com.gtsn.lib.api.ModPresence;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.LoadingModList;
import org.slf4j.Logger;

/**
 * 基于 Forge 的 {@link ModPresence} 默认实现。
 *
 * <p>同时覆盖两个时机：mod 构造期 {@link ModList} 尚未就绪时回退到
 * {@link FMLLoader#getLoadingModList()}；加载完成后使用 {@link ModList#isLoaded(String)}。</p>
 */
public final class ForgeModPresence implements ModPresence {

    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public boolean isLoaded(String modId) {
        ModList modList = ModList.get();
        if (modList != null) {
            return modList.isLoaded(modId);
        }
        LoadingModList loadingModList = FMLLoader.getLoadingModList();
        if (loadingModList != null) {
            return loadingModList.getModFileById(modId) != null;
        }
        LOGGER.warn("[GTSNLib] mod list not available yet while checking {}; treating as absent", modId);
        return false;
    }
}
