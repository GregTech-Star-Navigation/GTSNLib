package com.gtsn.lib.integration.mekanism;

import com.gtsn.lib.api.IntegrationModule;
import com.gtsn.lib.compat.mekanism.MekanismChemicals;
import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.ModList;
import org.slf4j.Logger;

/**
 * Mekanism 联动模块（#5 骨架，#14 化学注册接线）。
 *
 * <p>实现存在性判定与生命周期钩子。本类不引用任何 Mekanism 类型：化学注册的 Mekanism 翻译位于隔离包内由
 * 入口在构造期接线（{@code com.gtsn.lib.integration.mekanism.MekanismChemicalRegistration}），门面
 * {@link MekanismChemicals} 只暴露纯类型。按 ADR-0003，本模块只在目标 mod 确认在场后经
 * {@code Supplier<Supplier<IntegrationModule>>} 延迟实例化，缺席时不会触发类加载错误。</p>
 */
public final class MekanismIntegration implements IntegrationModule {

    public static final String MOD_ID = "mekanism";
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
        LOGGER.info("[GTSNLib] {} integration initialized; chemical registrations: {}",
                MOD_ID, MekanismChemicals.registrations().size());
    }

    @Override
    public String displayName() {
        return "Mekanism";
    }
}
