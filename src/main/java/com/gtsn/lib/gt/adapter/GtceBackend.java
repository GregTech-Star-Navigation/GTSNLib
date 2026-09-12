package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.gt.registration.FluidRegistration;
import com.gtsn.lib.gt.registration.FluidSpec;
import com.gtsn.lib.gt.registration.FluidState;
import com.gtsn.lib.gt.registration.MaterialComponent;
import com.gtsn.lib.gt.registration.MaterialPart;
import com.gtsn.lib.gt.registration.MaterialRegistration;
import com.gtsn.lib.gt.registration.MaterialSpec;
import com.gtsn.lib.gt.registration.RegistrationKind;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.Element;
import com.gregtechceu.gtceu.api.data.chemical.material.IMaterialRegistryManager;
import com.gregtechceu.gtceu.api.data.chemical.material.MarkerMaterial;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlag;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.registry.MaterialRegistry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.api.fluids.GTFluid;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKey;
import com.gregtechceu.gtceu.api.fluids.store.FluidStorageKeys;
import com.gregtechceu.gtceu.api.registry.GTRegistries;
import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;
import com.gregtechceu.gtceu.api.registry.registrate.IGTFluidBuilder;
import com.gregtechceu.gtceu.common.data.GTElements;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * {@link GtBackend} 的生产实现：**本包内唯一**直接接触 GTCEu 材料 / tag prefix / 注册 API 的类。
 *
 * <p>已验证的 GTCEu 7.5.3 访问点（tag {@code v7.5.3-1.20.1}，与 Gradle 解析的
 * {@code gtceu-1.20.1-7.5.3-slim.jar} 一致）：</p>
 * <ul>
 *   <li>{@code GTCEuAPI.materialManager}（{@code IMaterialRegistryManager}，注释注明 Construction 阶段可用）</li>
 *   <li>{@code IMaterialRegistryManager#getRegisteredMaterials()} / {@code #getMaterial(String)}</li>
 *   <li>{@code Material#getName()/getResourceLocation()/getUnlocalizedName()/getChemicalFormula()}</li>
 *   <li>{@code TagPrefix.values()} / {@code TagPrefix#name} / {@code #getLowerCaseName()} / {@code #getUnlocalizedName()}</li>
 *   <li>{@code IMaterialRegistryManager#getRegistry(String)} → {@code MaterialRegistry#getRegistrate()}
 *       （#15 通用注册入口；GTCEu 7.5.3 {@code CommonProxy#init()} 会对 {@code getRegistries()} 逐个调用
 *       {@code GTRegistrate#registerEventListeners(IEventBus)}，故该实例的条目会被真正注册；孤立的
 *       {@code GTRegistrate.create(String)} 仅构造实例、不挂事件总线）</li>
 *   <li>{@code GTRegistries.MACHINES}（{@code GTRegistry$RL<MachineDefinition>}，查询方法
 *       {@code containKey(ResourceLocation)}；7.5.3 无 {@code MATERIALS}/{@code TAG_PREFIXES} 字段）</li>
 * </ul>
 *
 * <p>材料注册简化层的翻译点（#12）在本类补齐，全部经真实源码核对：</p>
 * <ul>
 *   <li>材料构造：{@code new Material.Builder(ResourceLocation)}；没有 {@code .id()/.name()} 流式方法</li>
 *   <li>锭/粉：{@code Material.Builder#ingot()/#dust()}（属性驱动，{@code ingot()} 附带粉属性）</li>
 *   <li>板/杆/块/齿轮/螺栓/环/箔：{@code MaterialFlags.GENERATE_*} 标志驱动
 *       （板={@code GENERATE_PLATE}，杆={@code GENERATE_ROD}，块={@code FORCE_GENERATE_BLOCK}，
 *       齿轮={@code GENERATE_GEAR}，螺栓/螺丝={@code GENERATE_BOLT_SCREW}，环={@code GENERATE_RING}，
 *       箔={@code GENERATE_FOIL}）</li>
 *   <li>颜色/图标集：{@code Material.Builder#color(int)} / {@code #iconSet(MaterialIconSet)} 与
 *       {@code MaterialIconSet.getByName(String)}</li>
 *   <li>元素：{@code Material.Builder#element(Element)} 与 {@code GTElements.get(String)}</li>
 *   <li>组分：{@code Material.Builder#components(Object...)}（Material 名与数量成对）</li>
 *   <li>矿词：{@code ChemicalHelper.getTag(TagPrefix, Material)} → {@code TagKey<Item>}；
 *       物品：{@code ChemicalHelper.get(TagPrefix, Material, int)}</li>
 * </ul>
 *
 * <p>通用注册简化层的翻译点（#15）亦经真实签名核对：方块
 * {@code GTRegistrate#block(String, NonNullFunction)} → {@code GTBlockBuilder}
 * （{@code properties/lang/defaultLang/defaultLoot/simpleItem/register}）；物品
 * {@code AbstractRegistrate#item(String, NonNullFunction)} → {@code ItemBuilder}
 * （{@code properties/defaultModel/defaultLang/lang/register}）；机器
 * {@code GTRegistrate#machine(String, Function<IMachineBlockEntity, MetaMachine>)} →
 * {@code MachineBuilder}（{@code tier/rotationState/langValue/register}，{@code register()} 返回
 * {@code MachineDefinition}）。</p>
 *
 * <p>注意：7.5.3 的 {@code GTRegistries} **没有** {@code MATERIALS}/{@code TAG_PREFIXES} 字段
 * （已用 {@code javap} 对解析到的 slim jar 验证）；材料注册表经 {@code GTCEuAPI.materialManager} 访问。
 * 8.0 移除 {@code materialManager} 时，迁移点收敛在本类。</p>
 */
final class GtceBackend implements GtBackend {

    /**
     * 独立（无材料）流体的元数据载体。
     *
     * <p>GTCEu 7.5.3 的 {@code GTRegistrate#createFluid} 需要一个 {@link Material} 仅用于
     * 语言键 / 贴图命名；{@link MarkerMaterial} 不进入材料注册表、不参与任何物品生成（见
     * {@code MarkerMaterial#registerMaterial()} 为空实现），因此是承载一次性流体的干净载体。</p>
     */
    private static final Material STANDALONE_CARRIER =
            new MarkerMaterial(new ResourceLocation("gtsnlib", "standalone_fluid"));

    /** 一次性流体默认贴图（GTCEu 自带的中性流体贴图，附属 mod 可另行提供自定义贴图）。 */
    private static final ResourceLocation DEFAULT_STILL =
            new ResourceLocation("gtceu", "block/fluids/fluid.air");

    @Override
    public boolean available() {
        return GTCEuAPI.materialManager != null;
    }

    @Override
    public Collection<GtMaterialRef> materials() {
        IMaterialRegistryManager manager = GTCEuAPI.materialManager;
        if (manager == null) {
            return List.of();
        }
        List<GtMaterialRef> refs = new ArrayList<>();
        for (Material material : manager.getRegisteredMaterials()) {
            ResourceLocation id = material.getResourceLocation();
            refs.add(new GtMaterialRef(
                    material.getName(),
                    id.getNamespace(),
                    id.getPath(),
                    id.toString(),
                    material.getUnlocalizedName(),
                    material.getChemicalFormula()));
        }
        return refs;
    }

    @Override
    public Collection<GtTagPrefixRef> tagPrefixes() {
        List<GtTagPrefixRef> refs = new ArrayList<>();
        for (TagPrefix prefix : TagPrefix.values()) {
            refs.add(new GtTagPrefixRef(prefix.name, prefix.getLowerCaseName(), prefix.getUnlocalizedName()));
        }
        return refs;
    }

    @Override
    public GtRegistrateHandle registrate(String modId) {
        return GtRegistrateHandle.of(modId, requireRegistry(modId).getRegistrate());
    }

    @Override
    public GtRegistrateHandle createRegistrate(String modId) {
        IMaterialRegistryManager manager = GTCEuAPI.materialManager;
        if (manager == null) {
            throw new IllegalStateException("cannot create GTCEu material registry for '" + modId
                    + "': the material registry manager is unavailable. MaterialRegistryEvent only fires after"
                    + " GTCEu has initialized during Construction; call createRegistrate from that event."
                    + " See docs/registration.md.");
        }
        try {
            manager.createRegistry(modId);
        } catch (RuntimeException failure) {
            throw new IllegalStateException("failed to create GTCEu material registry for namespace '" + modId
                    + "'. createRegistrate must be called exactly once from MaterialRegistryEvent, the only phase"
                    + " in which GTCEu permits createRegistry (registry manager Phase.PRE). See docs/registration.md.",
                    failure);
        }
        return registrate(modId);
    }

    @Override
    public MaterialRegistration registerMaterial(MaterialSpec spec) {
        requireRegistry(spec.namespace());
        Material material = buildMaterial(spec);

        Map<String, String> derivedItems = new LinkedHashMap<>();
        List<String> oreTags = new ArrayList<>();
        for (MaterialPart part : spec.parts()) {
            TagPrefix prefix = prefixFor(part);
            derivedItems.put(part.key(), GtNames.derivedItemId(part.key(), material.getName()));
            TagKey<Item> tag = ChemicalHelper.getTag(prefix, material);
            if (tag != null) {
                oreTags.add(tag.location().toString());
            }
        }

        // 材料流体形态的 GT 资源位置在其构建期即可确定性推导：#13 的“与材料注册正确关联”。
        Map<String, String> fluids = new LinkedHashMap<>();
        for (FluidState state : FluidState.values()) {
            if (!spec.fluidStates().contains(state)) {
                continue;
            }
            FluidStorageKey key = storageKeyFor(state);
            String path = key.getRegistryNameFor(material);
            fluids.put(state.key(), spec.namespace() + ":" + path);
        }
        return new MaterialRegistration(spec.id(), spec.namespace(), spec.key(), derivedItems, oreTags, fluids);
    }

    @Override
    public FluidRegistration registerFluid(FluidSpec spec) {
        MaterialRegistry registry = requireRegistry(spec.namespace());

        Material material = spec.material()
                .map(link -> {
                    Material linked = GTCEuAPI.materialManager.getMaterial(link.key());
                    if (linked == null) {
                        throw new IllegalArgumentException("fluid '" + spec.key()
                                + "' references unknown GTCEu material: " + link.key());
                    }
                    return linked;
                })
                .orElse(STANDALONE_CARRIER);

        GTRegistrate registrate = registry.getRegistrate();
        IGTFluidBuilder builder = registrate
                .createFluid(spec.id(), "gtceu.fluid.generic", material, DEFAULT_STILL, DEFAULT_STILL)
                .temperature(spec.temperature())
                .density(spec.density())
                .luminance(spec.luminosity())
                .viscosity(spec.viscosity())
                .burnTime(spec.burnTime())
                .hasBlock(spec.hasBlock())
                .hasBucket(spec.hasBucket())
                .color(spec.color() | 0xFF000000)
                .state(gtStateFor(spec.state()));
        builder.registerFluid();

        String materialKey = spec.material().map(FluidSpec.MaterialLink::key).orElse("");
        return new FluidRegistration(spec.id(), spec.namespace(), spec.key(), spec.state(),
                materialKey, spec.namespace() + ":" + spec.id());
    }

    @Override
    public GtFluidStatus fluidStatus(String fluidId) {
        String query = fluidId == null ? "" : fluidId;
        boolean available = GTCEuAPI.materialManager != null;
        ResourceLocation location = parseFluidLocation(query);
        if (location == null) {
            return GtFluidStatus.missing(query, available, "", "", "");
        }
        Fluid fluid = ForgeRegistries.FLUIDS.getValue(location);
        if (fluid == null) {
            return GtFluidStatus.missing(query, available, "", "", "");
        }
        String stateKey = fluid instanceof GTFluid gtFluid
                ? gtFluid.getState().name().toLowerCase(Locale.ROOT)
                : "";
        return GtFluidStatus.present(query, available, location.toString(), stateKey, "");
    }

    @Override
    public List<GtPartStatus> partStatus(String materialId, Set<MaterialPart> parts) {
        IMaterialRegistryManager manager = GTCEuAPI.materialManager;
        if (manager == null) {
            return List.of();
        }
        Material material = manager.getMaterial(materialId);
        if (material == null) {
            return List.of();
        }
        List<GtPartStatus> statuses = new ArrayList<>(parts.size());
        for (MaterialPart part : parts) {
            TagPrefix prefix = prefixFor(part);
            TagKey<Item> tag = ChemicalHelper.getTag(prefix, material);
            ItemStack stack = ChemicalHelper.get(prefix, material, 1);
            statuses.add(new GtPartStatus(
                    part.key(),
                    GtNames.derivedItemId(part.key(), material.getName()),
                    tag == null ? "" : tag.location().toString(),
                    stack != null && !stack.isEmpty()));
        }
        return List.copyOf(statuses);
    }

    @Override
    public GtContentStatus contentStatus(RegistrationKind kind, String id) {
        String query = id == null ? "" : id.trim();
        boolean available = kind != RegistrationKind.MACHINE || GTRegistries.MACHINES != null;
        ResourceLocation location = parseContentLocation(query);
        if (location == null) {
            return GtContentStatus.missing(kind, query, available);
        }
        boolean present = switch (kind) {
            case BLOCK -> ForgeRegistries.BLOCKS.containsKey(location);
            case ITEM -> ForgeRegistries.ITEMS.containsKey(location);
            case MACHINE -> GTRegistries.MACHINES.containKey(location);
        };
        return present
                ? GtContentStatus.present(kind, query, location.toString(), available)
                : GtContentStatus.missing(kind, query, available);
    }

    /** 把声明式规格翻译为 GTCEu 的 {@code Material.Builder} 链并注册。 */
    private static Material buildMaterial(MaterialSpec spec) {
        Material.Builder builder = new Material.Builder(new ResourceLocation(spec.namespace(), spec.id()));
        builder.color(spec.color());

        MaterialIconSet iconSet = MaterialIconSet.getByName(spec.iconSet().key());
        if (iconSet == null) {
            throw new IllegalArgumentException("unknown GTCEu material icon set: " + spec.iconSet().key());
        }
        builder.iconSet(iconSet);

        Set<MaterialFlag> flags = new LinkedHashSet<>();
        for (MaterialPart part : spec.parts()) {
            switch (part) {
                case INGOT -> builder.ingot();
                case DUST -> builder.dust();
                case PLATE -> flags.add(MaterialFlags.GENERATE_PLATE);
                case ROD -> flags.add(MaterialFlags.GENERATE_ROD);
                case BLOCK -> flags.add(MaterialFlags.FORCE_GENERATE_BLOCK);
                case GEAR -> flags.add(MaterialFlags.GENERATE_GEAR);
                case SCREW, BOLT -> flags.add(MaterialFlags.GENERATE_BOLT_SCREW);
                case RING -> flags.add(MaterialFlags.GENERATE_RING);
                case FOIL -> flags.add(MaterialFlags.GENERATE_FOIL);
            }
        }
        if (!flags.isEmpty()) {
            builder.flags(flags.toArray(new MaterialFlag[0]));
        }

        spec.element().ifPresent(elementName -> {
            Element element = GTElements.get(elementName);
            if (element == null) {
                throw new IllegalArgumentException("unknown GTCEu element: " + elementName);
            }
            builder.element(element);
        });

        if (!spec.components().isEmpty()) {
            List<Object> pairs = new ArrayList<>(spec.components().size() * 2);
            for (MaterialComponent component : spec.components()) {
                pairs.add(component.material());
                pairs.add(component.amount());
            }
            builder.components(pairs.toArray());
        }

        // 材料流体形态（#13）：按固定顺序声明，保证主物态确定，流体资源位置可推导。
        for (FluidState state : FluidState.values()) {
            if (!spec.fluidStates().contains(state)) {
                continue;
            }
            switch (state) {
                case LIQUID -> builder.liquid();
                case GAS -> builder.gas();
                case PLASMA -> builder.plasma();
            }
        }

        return builder.buildAndRegister();
    }

    /** 校验并返回给定命名空间的 GTCEu 材料注册表（缺失时给出可诊断错误）。 */
    private static MaterialRegistry requireRegistry(String namespace) {
        IMaterialRegistryManager manager = GTCEuAPI.materialManager;
        if (manager == null) {
            throw new IllegalStateException("GTCEu material registry manager is unavailable; GTSNLib registration"
                    + " helpers require GTCEu to have initialized its registry manager (Construction)."
                    + " See docs/registration.md.");
        }
        MaterialRegistry registry = manager.getRegistry(namespace);
        if (registry == null || !namespace.equals(registry.getModid())) {
            throw new IllegalStateException("no GTCEu material registry for namespace '" + namespace
                    + "'. Every GTSNLib registration helper (registerBlock/registerItem/registerMachine/"
                    + "registerFluid/registerMaterial) resolves the namespace's GTCEu MaterialRegistry first,"
                    + " even when registering no materials. A namespace must create its registry during GTCEu"
                    + " MaterialRegistryEvent—the only phase in which GTCEuAPI.materialManager.createRegistry is"
                    + " allowed (registry manager Phase.PRE). Handle MaterialRegistryEvent on the mod event bus and"
                    + " call GtAdapter.get().createRegistrate(\"" + namespace + "\") (or"
                    + " GTCEuAPI.materialManager.createRegistry(\"" + namespace + "\")). See docs/registration.md.");
        }
        return registry;
    }

    /** 声明式物态 → GTCEu 流体存储键。 */
    private static FluidStorageKey storageKeyFor(FluidState state) {
        return switch (state) {
            case LIQUID -> FluidStorageKeys.LIQUID;
            case GAS -> FluidStorageKeys.GAS;
            case PLASMA -> FluidStorageKeys.PLASMA;
        };
    }

    /** 声明式物态 → GTCEu 流体物态。 */
    private static com.gregtechceu.gtceu.api.fluids.FluidState gtStateFor(FluidState state) {
        return switch (state) {
            case LIQUID -> com.gregtechceu.gtceu.api.fluids.FluidState.LIQUID;
            case GAS -> com.gregtechceu.gtceu.api.fluids.FluidState.GAS;
            case PLASMA -> com.gregtechceu.gtceu.api.fluids.FluidState.PLASMA;
        };
    }

    /** 解析流体资源位置；裸路径默认补全 GTSNLib 命名空间。非法输入返回 {@code null}。 */
    private static ResourceLocation parseFluidLocation(String query) {
        String raw = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (raw.isEmpty()) {
            return null;
        }
        String candidate = raw.indexOf(':') >= 0 ? raw : "gtsnlib:" + raw;
        return ResourceLocation.tryParse(candidate);
    }

    /** 解析通用注册条目的资源位置；裸路径默认补全 GTSNLib 命名空间。非法输入返回 {@code null}。 */
    private static ResourceLocation parseContentLocation(String query) {
        String raw = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (raw.isEmpty()) {
            return null;
        }
        String candidate = raw.indexOf(':') >= 0 ? raw : "gtsnlib:" + raw;
        return ResourceLocation.tryParse(candidate);
    }

    /** 声明部件 → GTCEu tag prefix。每个部件都有独立、已验证的映射。 */
    private static TagPrefix prefixFor(MaterialPart part) {
        return switch (part) {
            case INGOT -> TagPrefix.ingot;
            case PLATE -> TagPrefix.plate;
            case DUST -> TagPrefix.dust;
            case ROD -> TagPrefix.rod;
            case BLOCK -> TagPrefix.block;
            case GEAR -> TagPrefix.gear;
            case SCREW -> TagPrefix.screw;
            case BOLT -> TagPrefix.bolt;
            case RING -> TagPrefix.ring;
            case FOIL -> TagPrefix.foil;
        };
    }
}
