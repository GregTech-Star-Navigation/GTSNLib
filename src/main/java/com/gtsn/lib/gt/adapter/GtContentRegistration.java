package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.gt.registration.BlockRegistration;
import com.gtsn.lib.gt.registration.BlockSpec;
import com.gtsn.lib.gt.registration.ItemRegistration;
import com.gtsn.lib.gt.registration.ItemSpec;
import com.gtsn.lib.gt.registration.MachineRegistration;
import com.gtsn.lib.gt.registration.MachineSpec;
import com.gtsn.lib.gt.registration.RegistrationKind;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.material.event.MaterialEvent;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;

/**
 * 通用注册简化层（#15）的游戏内接线：通过声明式 helper 注册一个演示方块（含方块物品）、一个演示物品与
 * 一台最小机器，作为游戏内证据。
 *
 * <p><b>时机（已核对 GTCEu 7.5.3 源码）</b>：方块与物品经命名空间材料注册表的 registrate 排队，
 * 在 addon 材料注册时机 {@link MaterialEvent} 声明即可——GTCEu {@code CommonProxy#init()} 随后会为该
 * registrate 调用 {@code registerEventListeners(...)}，条目在 Minecraft {@code RegisterEvent} 时真正注册。</p>
 *
 * <p>机器则不同：{@code MachineBuilder#register()} 直接写入 {@code GTRegistries.MACHINES}，而
 * {@code GTRegistry} 的 {@code frozen} 初值为 {@code true}，且 {@code registerOrOverride} 仅以
 * {@code frozen} 判定是否拒绝。GTCEu 仅在 {@code GTMachines} 静态初始化期间
 * {@code unfreeze()}，并在发出 {@link GTCEuAPI.RegisterEvent} 之后立即 {@code freeze()}
 * （GTCEu 7.5.3 {@code GTMachines}：先发 {@code RegisterEvent<MachineDefinition>}，随后
 * {@code GTRegistries.MACHINES.freeze()}）。因此 addon 注册机器的唯一窗口是监听该事件；在
 * {@link MaterialEvent} 期间 {@code GTRegistries.MACHINES} 仍为默认冻结态，注册会被拒绝。</p>
 *
 * <p>该事件是 {@code GenericEvent}，Forge 要求经 {@code IEventBus#addGenericListener} 注册，
 * 不能用 {@code @SubscribeEvent} 自动订阅（实测后者不会触发）。入口 {@code GTSNLib} 构造函数调用
 * {@link #subscribe(IEventBus)}，从而把 GTCEu 类型保持在适配层内（ADR-0005）。</p>
 *
 * <p>本类位于适配层，因此允许直接引用 GTCEu 事件类型（ADR-0005）。</p>
 */
@Mod.EventBusSubscriber(modid = GTSNLib.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class GtContentRegistration {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String NAMESPACE = GTSNLib.MOD_ID;

    private GtContentRegistration() {
    }

    /**
     * 把机器注册监听挂到 mod 事件总线。由入口 {@code GTSNLib} 在构造期调用。
     *
     * <p>只暴露 {@link IEventBus}（Forge 类型），入口无需接触任何 GTCEu 类型。</p>
     */
    public static void subscribe(IEventBus modEventBus) {
        modEventBus.addGenericListener(MachineDefinition.class, GtContentRegistration::onMachineRegister);
    }

    /** addon 材料注册时机：声明演示方块（含方块物品）与演示物品（不涉及 GTRegistry 冻结窗口）。 */
    @SubscribeEvent
    public static void onMaterial(MaterialEvent event) {
        GtAdapter adapter = GtAdapter.get();

        BlockRegistration block = adapter.registerBlock(BlockSpec.builder(NAMESPACE, GtAdapter.DEMO_BLOCK_ID)
                .strength(2.0F, 3.0F)
                .withItem(true)
                .displayName("Test Block")
                .build());
        ItemRegistration item = adapter.registerItem(ItemSpec.builder(NAMESPACE, GtAdapter.DEMO_ITEM_ID)
                .maxStackSize(16)
                .displayName("Test Item")
                .build());

        LOGGER.info("[GTSNLib] registered demo block {} (item {}) and item {} via registration helper",
                block.resourceLocation(), block.itemResourceLocation(), item.resourceLocation());
    }

    /**
     * GTCEu 机器注册窗口：{@code gtceu:machine} 未冻结时（GTCEu 自身机器注册期间）声明演示机器。
     *
     * <p>防御性守卫：仅当 {@code GTRegistries.MACHINES} 未冻结且尚未登记该机器时执行，避免在其它
     * {@code RegisterEvent} 或重复触发时误注册。</p>
     */
    static void onMachineRegister(
            GTCEuAPI.RegisterEvent<ResourceLocation, MachineDefinition> event) {
        if (GTRegistries.MACHINES.isFrozen()) {
            return;
        }
        GtAdapter adapter = GtAdapter.get();
        if (adapter.isRegistered(RegistrationKind.MACHINE, GtAdapter.DEMO_MACHINE)) {
            return;
        }
        MachineRegistration machine = adapter.registerMachine(MachineSpec.builder(NAMESPACE, GtAdapter.DEMO_MACHINE_ID)
                .tier(1)
                .displayName("Test Machine")
                .build());
        LOGGER.info("[GTSNLib] registered demo machine {} via registration helper", machine.resourceLocation());
    }
}
