package com.gtsn.lib.integration.mekanism;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.compat.mekanism.ChemicalKind;
import com.gtsn.lib.compat.mekanism.ChemicalRegistration;
import com.gtsn.lib.compat.mekanism.ChemicalSpec;
import com.gtsn.lib.compat.mekanism.MekanismChemicals;
import com.gtsn.lib.core.DemoContentRuntime;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.IEventBus;
import org.slf4j.Logger;

/**
 * Mekanism 化学注册的游戏内接线（#14）：在 {@code @Mod} 构造期（Forge {@code RegisterEvent} 之前）安装
 * 化学后端并把演示化学物质经声明式 DSL 排队，注册事件期间由 Mekanism 的 {@code DeferredRegister} 真正写入。
 *
 * <p><b>时序（已核对 Forge 1.20.1-47.4.0 源码）</b>：{@code DeferredRegister#register(IEventBus)} 向 mod
 * 事件总线注册一个 {@code @SubscribeEvent RegisterEvent} 监听器；条目必须在 {@code RegisterEvent} 触发前
 * 排队（事件后 {@code DeferredRegister#register} 会抛
 * {@code "Cannot register new entries ... after RegisterEvent has been fired"}）。{@code FMLCommonSetupEvent}
 * 在注册事件之后，故接线必须发生在入口 {@code GTSNLib} 构造期。</p>
 *
 * <p>按 ADR-0003，本类只由入口在 {@code ModPresence} 判定 Mekanism 在场后经分支调用（缺席时类不被链接）；
 * 门面 {@link MekanismChemicals}、命令与 GameTest 均只依赖纯类型，不引用 Mekanism。</p>
 */
public final class MekanismChemicalRegistration {

    private static final Logger LOGGER = LogUtils.getLogger();

    private MekanismChemicalRegistration() {
    }

    /**
     * 安装化学后端、挂接注册表事件，并声明演示化学物质。由入口 {@code GTSNLib} 在 Mekanism 确认在场后调用。
     */
    public static void subscribe(IEventBus modEventBus) {
        MekanismChemicalBackend backend = new MekanismChemicalBackend(GTSNLib.MOD_ID);
        backend.registerTo(modEventBus);
        MekanismChemicals.install(backend);

        // 演示门控：后端恒安装（供附属 mod 消费），仅演示化学物质的登记受门控约束。
        if (!DemoContentRuntime.enabled()) {
            LOGGER.info("[GTSNLib] demo content disabled; skipping Mekanism demo chemical {}", 
                    MekanismChemicals.DEMO_CHEMICAL_ID);
            return;
        }

        ChemicalRegistration registration = MekanismChemicals.register(
                ChemicalSpec.builder(GTSNLib.MOD_ID, MekanismChemicals.DEMO_CHEMICAL_ID)
                        .kind(ChemicalKind.GAS)
                        .tint(0x88CCFF)
                        .build());
        LOGGER.info("[GTSNLib] registered Mekanism chemical {} @ {} (registry {}) via chemical DSL",
                registration.key(), registration.resourceLocation(), registration.registryId());
    }
}
