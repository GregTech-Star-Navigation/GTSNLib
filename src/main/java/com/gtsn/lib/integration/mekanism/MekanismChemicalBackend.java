package com.gtsn.lib.integration.mekanism;

import com.gtsn.lib.compat.mekanism.ChemicalBackend;
import com.gtsn.lib.compat.mekanism.ChemicalKind;
import com.gtsn.lib.compat.mekanism.ChemicalRegistration;
import com.gtsn.lib.compat.mekanism.ChemicalSpec;
import com.gtsn.lib.compat.mekanism.ChemicalStatus;

import mekanism.api.MekanismAPI;
import mekanism.api.chemical.Chemical;
import mekanism.api.chemical.ChemicalBuilder;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasBuilder;
import mekanism.api.chemical.infuse.InfuseType;
import mekanism.api.chemical.infuse.InfuseTypeBuilder;
import mekanism.api.chemical.pigment.Pigment;
import mekanism.api.chemical.pigment.PigmentBuilder;
import mekanism.api.chemical.slurry.SlurryBuilder;
import mekanism.common.registration.impl.GasDeferredRegister;
import mekanism.common.registration.impl.InfuseTypeDeferredRegister;
import mekanism.common.registration.impl.PigmentDeferredRegister;
import mekanism.common.registration.impl.SlurryDeferredRegister;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.IForgeRegistry;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * {@link ChemicalBackend} 的生产实现：**本包内唯一**直接接触 Mekanism 化学 API 的类（#14）。
 *
 * <p>已验证的 Mekanism 1.20.1-10.4.16.80 访问点（对 Gradle 解析到的
 * {@code Mekanism-1.20.1-10.4.16.80.jar} 用 {@code javap} 核对，并对照上游 tag
 * {@code v1.20.1-10.4.16.80} 的 {@code src/api/java/mekanism/api/MekanismAPI.java} 源码）：</p>
 * <ul>
 *   <li>注册表：{@code MekanismAPI.gasRegistry()} / {@code slurryRegistry()} /
 *       {@code infuseTypeRegistry()} / {@code pigmentRegistry()}，均返回
 *       {@code net.minecraftforge.registries.IForgeRegistry<T>}（在 {@code NewRegistryEvent} 之前可能为
 *       {@code null}）；同时提供 {@code GAS_REGISTRY_NAME} 等 {@code ResourceKey}。</li>
 *   <li>注册入口：{@code GasDeferredRegister}/{@code SlurryDeferredRegister}/
 *       {@code InfuseTypeDeferredRegister}/{@code PigmentDeferredRegister}（构造入参 modid，内部以
 *       {@code DeferredRegister.create(ResourceKey, modid)} 建立，故可在 {@code NewRegistryEvent} 前创建），
 *       经 {@link WrappedDeferredRegister#register(IEventBus)} 挂到 mod 事件总线的 {@code RegisterEvent}。</li>
 *   <li>构建器：{@code GasBuilder.builder()}（默认贴图 {@code mekanism:liquid/liquid}）、
 *       {@code InfuseTypeBuilder.builder()}、{@code PigmentBuilder.builder()}、
 *       {@code SlurryBuilder.dirty()/clean()}；公共 {@code ChemicalBuilder#tint(int)} / {@code hidden()}。
 *       浆液经 {@code SlurryDeferredRegister#register(baseName, UnaryOperator<SlurryBuilder>)} 成对生成
 *       {@code dirty_<base>} 与 {@code clean_<base>}。</li>
 * </ul>
 *
 * <p>所有条目在 {@code @Mod} 构造期（{@code RegisterEvent} 之前）排队，注册事件期间真正写入注册表；因此
 * 安装必须在构造期完成，见 {@link MekanismChemicalRegistration#subscribe(IEventBus)}。</p>
 */
final class MekanismChemicalBackend implements ChemicalBackend {

    private final String namespace;
    private final GasDeferredRegister gases;
    private final SlurryDeferredRegister slurries;
    private final InfuseTypeDeferredRegister infuseTypes;
    private final PigmentDeferredRegister pigments;
    private final Map<String, ChemicalRegistration> registered = new LinkedHashMap<>();

    MekanismChemicalBackend(String namespace) {
        this.namespace = Objects.requireNonNull(namespace, "namespace");
        this.gases = new GasDeferredRegister(namespace);
        this.slurries = new SlurryDeferredRegister(namespace);
        this.infuseTypes = new InfuseTypeDeferredRegister(namespace);
        this.pigments = new PigmentDeferredRegister(namespace);
    }

    /** 把四个化学注册表挂到 mod 事件总线；必须在 {@code RegisterEvent} 触发前调用。 */
    void registerTo(IEventBus modEventBus) {
        Objects.requireNonNull(modEventBus, "modEventBus");
        gases.register(modEventBus);
        slurries.register(modEventBus);
        infuseTypes.register(modEventBus);
        pigments.register(modEventBus);
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public ChemicalRegistration register(ChemicalSpec spec) {
        Objects.requireNonNull(spec, "spec");
        ChemicalKind kind = spec.kind();
        List<String> locations = new ArrayList<>(kind == ChemicalKind.SLURRY ? 2 : 1);
        switch (kind) {
            case GAS -> {
                gases.register(spec.id(), () -> new Gas(apply(GasBuilder.builder(), spec)));
                locations.add(resourceLocation(spec.namespace(), spec.id()));
            }
            case SLURRY -> {
                slurries.register(spec.id(), builder -> applyOre(apply(builder, spec), spec));
                locations.add(resourceLocation(spec.namespace(), "dirty_" + spec.id()));
                locations.add(resourceLocation(spec.namespace(), "clean_" + spec.id()));
            }
            case INFUSE_TYPE -> {
                infuseTypes.register(spec.id(), () -> new InfuseType(apply(InfuseTypeBuilder.builder(), spec)));
                locations.add(resourceLocation(spec.namespace(), spec.id()));
            }
            case PIGMENT -> {
                pigments.register(spec.id(), () -> new Pigment(apply(PigmentBuilder.builder(), spec)));
                locations.add(resourceLocation(spec.namespace(), spec.id()));
            }
        }
        ChemicalRegistration registration = new ChemicalRegistration(spec.id(), spec.namespace(), spec.key(),
                kind, kind.registryId(), spec.tint(), spec.hidden(), locations);
        registered.put(spec.key(), registration);
        return registration;
    }

    @Override
    public ChemicalStatus status(String id) {
        String query = id == null ? "" : id.trim();
        ChemicalRegistration declared = declared(query);
        if (declared != null) {
            for (String location : declared.resourceLocations()) {
                Chemical<?> chemical = lookup(declared.kind(), location);
                if (chemical != null) {
                    return ChemicalStatus.present(query, true, declared.kind().key(), declared.registryId(),
                            location, chemical.getTint(), chemical.isHidden());
                }
            }
            return ChemicalStatus.missing(query, true, declared.kind().key(), declared.registryId());
        }
        for (ChemicalKind kind : ChemicalKind.values()) {
            for (String candidate : candidateLocations(query)) {
                Chemical<?> chemical = lookup(kind, candidate);
                if (chemical != null) {
                    return ChemicalStatus.present(query, true, kind.key(), kind.registryId(),
                            candidate, chemical.getTint(), chemical.isHidden());
                }
            }
        }
        return ChemicalStatus.missing(query, true);
    }

    /** 解析已声明条目：完整键、裸 id、或任一真实资源位置。 */
    private ChemicalRegistration declared(String query) {
        if (query.isEmpty()) {
            return null;
        }
        ChemicalRegistration exact = registered.get(query);
        if (exact != null) {
            return exact;
        }
        if (query.indexOf(':') < 0) {
            ChemicalRegistration byKey = registered.get(namespace + ":" + query);
            if (byKey != null) {
                return byKey;
            }
        }
        for (ChemicalRegistration registration : registered.values()) {
            if (registration.resourceLocations().contains(query)
                    || registration.id().equalsIgnoreCase(query)) {
                return registration;
            }
        }
        return null;
    }

    /** 未声明条目的候选资源位置（含浆液 dirty/clean 变体）。 */
    private List<String> candidateLocations(String query) {
        if (query.isEmpty()) {
            return List.of();
        }
        String candidate = query.indexOf(':') >= 0 ? query : namespace + ":" + query;
        ResourceLocation parsed = ResourceLocation.tryParse(candidate);
        if (parsed == null) {
            return List.of();
        }
        String path = parsed.getPath();
        List<String> locations = new ArrayList<>(3);
        locations.add(parsed.toString());
        if (!path.startsWith("dirty_") && !path.startsWith("clean_")) {
            locations.add(parsed.getNamespace() + ":dirty_" + path);
            locations.add(parsed.getNamespace() + ":clean_" + path);
        }
        return locations;
    }

    /** 在给定种类的真实注册表中查询条目；注册表未就绪或未命中返回 {@code null}。 */
    private static Chemical<?> lookup(ChemicalKind kind, String location) {
        IForgeRegistry<?> registry = registryFor(kind);
        if (registry == null) {
            return null;
        }
        ResourceLocation parsed = ResourceLocation.tryParse(location);
        if (parsed == null) {
            return null;
        }
        Object value = registry.getValue(parsed);
        return value instanceof Chemical<?> chemical ? chemical : null;
    }

    private static IForgeRegistry<?> registryFor(ChemicalKind kind) {
        return switch (kind) {
            case GAS -> MekanismAPI.gasRegistry();
            case SLURRY -> MekanismAPI.slurryRegistry();
            case INFUSE_TYPE -> MekanismAPI.infuseTypeRegistry();
            case PIGMENT -> MekanismAPI.pigmentRegistry();
        };
    }

    private static String resourceLocation(String namespace, String path) {
        return namespace + ":" + path;
    }

    /** 应用通用化学属性（颜色、隐藏）。 */
    private static <C extends Chemical<C>, B extends ChemicalBuilder<C, B>> B apply(B builder, ChemicalSpec spec) {
        builder.tint(spec.tint());
        if (spec.hidden()) {
            builder.hidden();
        }
        return builder;
    }

    /** 额外应用浆液矿词关联。 */
    private static SlurryBuilder applyOre(SlurryBuilder builder, ChemicalSpec spec) {
        spec.oreTag().ifPresent(ore -> builder.ore(new ResourceLocation(ore.namespace(), ore.id())));
        return builder;
    }
}
