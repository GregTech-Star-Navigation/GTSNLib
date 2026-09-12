package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.gt.registration.BlockRegistration;
import com.gtsn.lib.gt.registration.BlockSpec;
import com.gtsn.lib.gt.registration.ItemRegistration;
import com.gtsn.lib.gt.registration.ItemSpec;
import com.gtsn.lib.gt.registration.MachineRegistration;
import com.gtsn.lib.gt.registration.MachineSpec;

import com.gregtechceu.gtceu.api.data.RotationState;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.registry.registrate.GTBlockBuilder;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

import com.tterrag.registrate.builders.ItemBuilder;
import com.tterrag.registrate.util.entry.BlockEntry;
import com.tterrag.registrate.util.entry.ItemEntry;

import java.util.Objects;

/**
 * GT 注册入口句柄：库内对 GTCEu {@code GTRegistrate} 的稳定包装，也是通用注册简化层（#15）的
 * 翻译点。
 *
 * <p>本类只暴露 GTSN 自有声明类型（{@link BlockSpec}/{@link ItemSpec}/{@link MachineSpec}）与结果视图，
 * 因此调用方永远不会链接到 GTCEu 类型；GTCEu 的 {@code GTBlockBuilder}/{@code ItemBuilder}/
 * {@code MachineBuilder} 细节全部封装在此。上游迁移点（ADR-0005）：GTCEu 8.0 移除
 * {@code GTRegistrate} 相关 API 时，只需改本类的翻译实现。</p>
 *
 * <p><b>注册入口来源（已核对 GTCEu 7.5.3）</b>：必须使用命名空间材料注册表自带的 registrate
 * （{@code GTCEuAPI.materialManager.getRegistry(modId).getRegistrate()}）。GTCEu 在
 * {@code CommonProxy#init} 中对其 {@code getRegistries()} 逐个调用
 * {@code registerEventListeners(...)} 挂接 {@code RegisterEvent}，因此该实例的条目会被真正注册；
 * 而孤立的 {@code GTRegistrate.create(modId)} 从未挂接事件总线，仅调用 create 不会生效。</p>
 */
public final class GtRegistrateHandle {

    private final String modId;
    private final GTRegistrate registrate;

    private GtRegistrateHandle(String modId, GTRegistrate registrate) {
        this.modId = modId;
        this.registrate = registrate;
    }

    /**
     * 包装给定命名空间材料注册表自带的 {@link GTRegistrate}。
     *
     * <p>必须使用 {@code GTCEuAPI.materialManager.getRegistry(modId).getRegistrate()}：GTCEu 会为每个
     * 材料注册表的 registrate 挂接 {@code RegisterEvent} 监听（见 GTCEu 7.5.3 {@code CommonProxy#init}），
     * 而孤立的 {@code GTRegistrate.create(modId)} 从未挂接事件总线，其条目不会被注册。</p>
     */
    static GtRegistrateHandle of(String modId, GTRegistrate registrate) {
        Objects.requireNonNull(modId, "modId");
        return new GtRegistrateHandle(modId, Objects.requireNonNull(registrate, "registrate"));
    }

    /** 该注册入口所属的 ModID。 */
    public String modId() {
        return modId;
    }

    /**
     * 把声明式方块规格翻译为 GTCEu 方块注册链并立即登记到该 registrate。
     *
     * <p>翻译链：{@code block(id, Block::new).properties(strength/lightLevel/requiresCorrectTool).lang().loot()}
     * ，当 {@link BlockSpec#withItem()} 为真时追加 {@code simpleItem()} 生成可获取的方块物品。</p>
     */
    public BlockRegistration registerBlock(BlockSpec spec) {
        Objects.requireNonNull(spec, "spec");
        GTBlockBuilder<Block, GTRegistrate> builder = registrate
                .block(spec.id(), Block::new)
                .properties(properties -> applyBlockProperties(properties, spec));
        builder = spec.displayName().isPresent()
                ? builder.lang(spec.displayName().get())
                : builder.defaultLang();
        builder = builder.defaultLoot();
        if (spec.withItem()) {
            builder = builder.simpleItem();
        }
        BlockEntry<Block> entry = builder.register();
        String itemResourceLocation = spec.withItem() ? entry.getId().toString() : "";
        return new BlockRegistration(spec.id(), spec.namespace(), spec.key(), itemResourceLocation);
    }

    /**
     * 把声明式物品规格翻译为 GTCEu 物品注册链并立即登记到该 registrate。
     *
     * <p>翻译链：{@code item(id, Item::new).properties(stacksTo/fireResistant).model().lang()}。</p>
     */
    public ItemRegistration registerItem(ItemSpec spec) {
        Objects.requireNonNull(spec, "spec");
        ItemBuilder<Item, GTRegistrate> builder = registrate
                .item(spec.id(), Item::new)
                .properties(properties -> {
                    properties.stacksTo(spec.maxStackSize());
                    if (spec.fireResistant()) {
                        properties.fireResistant();
                    }
                    return properties;
                })
                .defaultModel();
        builder = spec.displayName().isPresent()
                ? builder.lang(spec.displayName().get())
                : builder.defaultLang();
        ItemEntry<Item> entry = builder.register();
        return new ItemRegistration(spec.id(), spec.namespace(), spec.key(), spec.maxStackSize());
    }

    /**
     * 把声明式机器规格翻译为 GTCEu 机器注册链并立即登记到该 registrate。
     *
     * <p>翻译链：{@code machine(id, GtsnTestMachine::new).tier(tier).rotationState(ALL).langValue(displayName)}
     * 。机器定义由 {@code MachineBuilder} 依据这些声明创建，机器方块/物品/方块实体类型由 GTCEu
     * 的标准机器模板（{@code MetaMachineBlock}/{@code MetaMachineItem}/{@code MetaMachineBlockEntity}）生成。</p>
     */
    public MachineRegistration registerMachine(MachineSpec spec) {
        Objects.requireNonNull(spec, "spec");
        MachineDefinition definition = registrate
                .machine(spec.id(), GtsnTestMachine::new)
                .tier(spec.tier())
                .rotationState(RotationState.ALL)
                .langValue(spec.displayName())
                .register();
        return new MachineRegistration(spec.id(), spec.namespace(), spec.key(), spec.tier());
    }

    /** 把声明式属性应用到 Minecraft 方块属性上。 */
    private static BlockBehaviour.Properties applyBlockProperties(BlockBehaviour.Properties properties,
                                                                   BlockSpec spec) {
        properties.strength(spec.destroyTime(), spec.explosionResistance());
        if (spec.lightLevel() > 0) {
            properties.lightLevel(state -> spec.lightLevel());
        }
        if (spec.requiresCorrectToolForDrops()) {
            properties.requiresCorrectToolForDrops();
        }
        return properties;
    }

    /**
     * 原始 GT 注册入口，供适配层内的注册翻译使用。
     * 有意保持包内可见：GTCEu 类型不得越过适配层边界。
     */
    GTRegistrate unwrap() {
        return registrate;
    }
}
