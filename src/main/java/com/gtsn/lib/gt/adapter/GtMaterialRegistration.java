package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.gt.registration.MaterialIcon;
import com.gtsn.lib.gt.registration.MaterialPart;
import com.gtsn.lib.gt.registration.MaterialRegistration;
import com.gtsn.lib.gt.registration.MaterialSpec;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialRegistryEvent;
import com.mojang.logging.LogUtils;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 材料注册简化层的 GTCEu 事件接线（#12）：在 GTCEu 的 Construction 阶段建立 GTSNLib 的材料注册表，
 * 并在 addon 材料注册时机通过声明式 DSL 注册一个演示材料，作为游戏内证据。
 *
 * <p>GTCEu 在 {@code CommonProxy.init()}（{@code FMLConstructModEvent} 之后）依次：发出
 * {@link MaterialRegistryEvent}（仅此时可 {@code createRegistry}）→ 解冻注册表 → 注册 GT 自身材料
 * → 发出 {@link MaterialEvent}（附属 mod 于此注册材料）→ 关闭 → 冻结。本类按此顺序接线。</p>
 *
 * <p>本类位于适配层，因此允许直接引用 GTCEu 事件类型（ADR-0005）。</p>
 */
@Mod.EventBusSubscriber(modid = GTSNLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GtMaterialRegistration {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAMESPACE = GTSNLib.MOD_ID;

    private GtMaterialRegistration() {
    }

    /** 为 GTSNLib 自身 namespace 建立 GTCEu 材料注册表（仅 PRE 阶段可建）。 */
    @SubscribeEvent
    public static void onMaterialRegistry(MaterialRegistryEvent event) {
        if (GTCEuAPI.materialManager == null) {
            return;
        }
        GTCEuAPI.materialManager.createRegistry(NAMESPACE);
        LOGGER.info("[GTSNLib] created GTCEu material registry for {}", NAMESPACE);
    }

    /** 通过声明式 DSL 注册演示材料，验证衍生件、矿词与配方钩子接缝。 */
    @SubscribeEvent
    public static void onMaterial(MaterialEvent event) {
        MaterialSpec spec = MaterialSpec.builder(NAMESPACE, GtAdapter.DEMO_MATERIAL_ID)
                .color(0x8A2BE2)
                .iconSet(MaterialIcon.METALLIC)
                .parts(MaterialPart.INGOT, MaterialPart.PLATE, MaterialPart.DUST, MaterialPart.ROD)
                .recipeHook(GtMaterialRegistration::logRecipeHook)
                .build();
        MaterialRegistration registration = GtAdapter.get().registerMaterial(spec);
        LOGGER.info("[GTSNLib] registered GTCEu material {} | derived parts: {} | ore tags: {}",
                registration.resourceLocation(), registration.derivedItems(), registration.oreTags());
    }

    private static void logRecipeHook(MaterialRegistration registration) {
        LOGGER.info("[GTSNLib] recipe hook invoked for {} (parts: {})",
                registration.resourceLocation(), registration.derivedItems().keySet());
    }
}
