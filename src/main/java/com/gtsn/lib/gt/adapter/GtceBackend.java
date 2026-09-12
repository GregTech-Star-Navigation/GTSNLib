package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.gt.registration.MaterialComponent;
import com.gtsn.lib.gt.registration.MaterialPart;
import com.gtsn.lib.gt.registration.MaterialRegistration;
import com.gtsn.lib.gt.registration.MaterialSpec;

import com.gregtechceu.gtceu.api.GTCEuAPI;
import com.gregtechceu.gtceu.api.data.chemical.ChemicalHelper;
import com.gregtechceu.gtceu.api.data.chemical.Element;
import com.gregtechceu.gtceu.api.data.chemical.material.IMaterialRegistryManager;
import com.gregtechceu.gtceu.api.data.chemical.material.Material;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlag;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialFlags;
import com.gregtechceu.gtceu.api.data.chemical.material.info.MaterialIconSet;
import com.gregtechceu.gtceu.api.data.chemical.material.registry.MaterialRegistry;
import com.gregtechceu.gtceu.api.data.tag.TagPrefix;
import com.gregtechceu.gtceu.common.data.GTElements;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
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
 *   <li>{@code GTRegistrate.create(String)}</li>
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
 * <p>注意：7.5.3 的 {@code GTRegistries} **没有** {@code MATERIALS}/{@code TAG_PREFIXES} 字段
 * （已用 {@code javap} 对解析到的 slim jar 验证）；材料注册表经 {@code GTCEuAPI.materialManager} 访问。
 * 8.0 移除 {@code materialManager} 时，迁移点收敛在本类。</p>
 */
final class GtceBackend implements GtBackend {

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
        return GtRegistrateHandle.create(modId);
    }

    @Override
    public MaterialRegistration registerMaterial(MaterialSpec spec) {
        IMaterialRegistryManager manager = GTCEuAPI.materialManager;
        if (manager == null) {
            throw new IllegalStateException("GTCEu material registry is unavailable");
        }
        MaterialRegistry registry = manager.getRegistry(spec.namespace());
        if (!spec.namespace().equals(registry.getModid())) {
            throw new IllegalStateException("no GTCEu material registry for namespace '" + spec.namespace()
                    + "'; create it during MaterialRegistryEvent before registering materials");
        }

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
        return new MaterialRegistration(spec.id(), spec.namespace(), spec.key(), derivedItems, oreTags);
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

        return builder.buildAndRegister();
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
