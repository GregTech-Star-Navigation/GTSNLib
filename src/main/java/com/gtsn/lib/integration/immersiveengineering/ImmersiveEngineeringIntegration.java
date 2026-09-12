package com.gtsn.lib.integration.immersiveengineering;

import com.gtsn.lib.api.IntegrationModule;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * 沉浸工程（Immersive Engineering）联动模块骨架（#6）。
 *
 * <p>当前只实现存在性判定与生命周期钩子，具体联动内容（多方块 / 电力）留待后续票。本骨架不引用任何
 * IE 类型；按 ADR-0003，它只在目标 mod 确认在场后经 {@code Supplier<Supplier<IntegrationModule>>}
 * 延迟实例化，缺席时不会触发类加载错误。</p>
 */
public final class ImmersiveEngineeringIntegration implements IntegrationModule {

    public static final String MOD_ID = "immersiveengineering";
    private static final Logger LOGGER = LogUtils.getLogger();

    @Override
    public String modId() {
        return MOD_ID;
    }

    @Override
    public boolean isPresent() {
        ModList modList = ModList.get();
        return modList != null && modList.isLoaded(MOD_ID);
    }

    @Override
    public void init() {
        LOGGER.info("[GTSNLib] {} integration initialized", MOD_ID);
    }

    @Override
    public String displayName() {
        return "Immersive Engineering";
    }
}
