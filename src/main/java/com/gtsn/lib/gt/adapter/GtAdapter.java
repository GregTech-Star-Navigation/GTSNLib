package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.gt.registration.FluidRegistration;
import com.gtsn.lib.gt.registration.FluidRegistrar;
import com.gtsn.lib.gt.registration.FluidSpec;
import com.gtsn.lib.gt.registration.MaterialPart;
import com.gtsn.lib.gt.registration.MaterialRegistration;
import com.gtsn.lib.gt.registration.MaterialRegistrar;
import com.gtsn.lib.gt.registration.MaterialSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * GTCEu 适配层门面：库内查询 GTCEu 材料 / tag prefix 与获取 GT 注册入口的唯一入口。
 *
 * <p><b>访问纪律</b>：除 {@code com.gtsn.lib.gt.adapter} 包外，GTSNLib 任何代码不得直接 import
 * GTCEu（{@code com.gregtechceu.gtceu.*}）；一切 GTCEu 访问必须经本门面。搜索验证：</p>
 * <pre>
 *   grep -rn "com.gregtechceu" src/main/java --include=*.java
 *   # 应仅命中 com/gtsn/lib/gt/adapter/ 下的适配层实现
 * </pre>
 *
 * <p>本门面只暴露 GTSN 自有类型（{@link GtMaterialRef}/{@link GtTagPrefixRef}/{@link GtQueryResult}/
 * {@link GtRegistrateHandle}），因此调用方永远不会链接到 GTCEu 类。上游 8.0 移除
 * {@code materialManager}/{@code MaterialRegistryEvent}/{@code registerRegistrate} 时，
 * 迁移面收敛在适配层内部（ADR-0005）。</p>
 *
 * <p>查询逻辑本身不接触 GTCEu：数据由后端 {@link GtBackend} 提供，故可用假后端纯单测。
 * 生产后端 {@link GtceBackend} 在首次 {@link #get()} 时惰性建立。</p>
 */
public final class GtAdapter {

    /** 运行时探针使用的已知 GT 材料（GTCEu 7.5.3 内置 Iron）。 */
    public static final String DEFAULT_PROBE_MATERIAL = "iron";

    /** 材料注册简化层在游戏内注册的演示材料路径（#12 证据）。 */
    public static final String DEMO_MATERIAL_ID = "star_alloy";

    /** 演示材料完整键 {@code gtsnlib:star_alloy}。 */
    public static final String DEMO_MATERIAL = "gtsnlib:" + DEMO_MATERIAL_ID;

    /** 演示用一次性（无材料）流体路径（#13 证据）。 */
    public static final String DEMO_FLUID_ID = "stellar_air";

    /** 演示用一次性流体完整键 {@code gtsnlib:stellar_air}。 */
    public static final String DEMO_FLUID = "gtsnlib:" + DEMO_FLUID_ID;

    private final GtBackend backend;
    private final MaterialRegistrar registrar;
    private final FluidRegistrar fluidRegistrar;

    GtAdapter(GtBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.registrar = new MaterialRegistrar(backend::registerMaterial);
        this.fluidRegistrar = new FluidRegistrar(backend::registerFluid);
    }

    /** 生产门面单例（惰性建立 {@link GtceBackend}）。 */
    public static GtAdapter get() {
        return Holder.INSTANCE;
    }

    /** GTCEu 材料注册表是否已就绪。 */
    public boolean available() {
        return backend.available();
    }

    /** 当前已注册材料的只读快照。 */
    public List<GtMaterialRef> materials() {
        return List.copyOf(backend.materials());
    }

    /** 当前 GTCEu tag prefix 的只读快照。 */
    public List<GtTagPrefixRef> tagPrefixes() {
        return List.copyOf(backend.tagPrefixes());
    }

    /** 当前 GTCEu tag prefix 总数。 */
    public int tagPrefixCount() {
        return backend.tagPrefixes().size();
    }

    /**
     * 按材料 id、材料名或完整资源位置查询材料（大小写不敏感）。
     *
     * @return 命中的材料视图；未命中或输入为空返回 {@link Optional#empty()}
     */
    public Optional<GtMaterialRef> findMaterial(String id) {
        if (id == null) {
            return Optional.empty();
        }
        String raw = id.trim();
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        String lowered = raw.toLowerCase(Locale.ROOT);
        String normalized = GtNames.normalizeName(raw);
        for (GtMaterialRef material : backend.materials()) {
            if (material.resourceLocation().equalsIgnoreCase(lowered)
                    || material.name().equalsIgnoreCase(lowered)
                    || material.name().equals(normalized)) {
                return Optional.of(material);
            }
        }
        return Optional.empty();
    }

    /**
     * 按原始驼峰名或下划线键查询 tag prefix（大小写不敏感），例如
     * {@code "tinyDust"} 与 {@code "tiny_dust"} 均可命中。
     */
    public Optional<GtTagPrefixRef> findTagPrefix(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String raw = name.trim();
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        String snake = GtNames.toSnakeCase(raw);
        for (GtTagPrefixRef prefix : backend.tagPrefixes()) {
            if (prefix.name().equalsIgnoreCase(raw) || prefix.lowerCaseName().equals(snake)) {
                return Optional.of(prefix);
            }
        }
        return Optional.empty();
    }

    /** 查询给定材料并汇总为稳定结果视图（供命令/日志使用）。 */
    public GtQueryResult query(String materialId) {
        String query = materialId == null ? "" : materialId;
        boolean available = backend.available();
        int tagPrefixes = tagPrefixCount();
        return findMaterial(query)
                .map(material -> GtQueryResult.present(query, available, material, tagPrefixes))
                .orElseGet(() -> GtQueryResult.missing(query, available, tagPrefixes));
    }

    /**
     * 获取给定 ModID 的 GT 注册入口（{@link GtRegistrateHandle}），供 #13（流体/气体/等离子体）使用。
     * 材料注册（#12）已由 {@link #registerMaterial(MaterialSpec)} 提供，材料注册表另行经
     * {@code GTCEuAPI.materialManager} 建立。
     */
    public GtRegistrateHandle registrate(String modId) {
        return backend.registrate(modId);
    }

    /**
     * 注册一种声明式材料：翻译为 GTCEu 注册、记录结果并回调配方钩子（材料注册简化层入口）。
     *
     * <p>同一 {@code namespace:id} 重复注册会被拒绝；GTCEu 注册失败时不会记录、也不会回调钩子。</p>
     *
     * @return 已生成材料的稳定结果视图（id、资源位置、衍生件、矿词）
     */
    public MaterialRegistration registerMaterial(MaterialSpec spec) {
        return registrar.register(spec);
    }

    /** 给定 {@code namespace:id} 是否已经本适配层注册。 */
    public boolean isMaterialRegistered(String key) {
        return registrar.isRegistered(key);
    }

    /** 按 {@code namespace:id} 查询已注册材料的结果视图。 */
    public Optional<MaterialRegistration> registeredMaterial(String key) {
        return registrar.registration(key);
    }

    /** 已注册材料的只读快照（按注册顺序）。 */
    public List<MaterialRegistration> registrations() {
        return registrar.registrations();
    }

    // ---- #13: fluid / gas / plasma ----

    /**
     * 注册一种声明式流体（独立或材料关联）：翻译为 GT 流体注册、记录结果并回调注册钩子。
     *
     * <p>同一 {@code namespace:id} 重复注册会被拒绝；GT 注册失败时不会记录、也不会回调钩子。</p>
     *
     * @return 已注册流体的稳定结果视图（id、物态、材料关联、真实 GT 流体资源位置）
     */
    public FluidRegistration registerFluid(FluidSpec spec) {
        return fluidRegistrar.register(spec);
    }

    /** 给定 {@code namespace:id} 是否已经本适配层注册。 */
    public boolean isFluidRegistered(String key) {
        return fluidRegistrar.isRegistered(key);
    }

    /** 按 {@code namespace:id} 查询已注册流体的结果视图。 */
    public Optional<FluidRegistration> registeredFluid(String key) {
        return fluidRegistrar.registration(key);
    }

    /** 已注册流体的只读快照（按注册顺序）。 */
    public List<FluidRegistration> fluidRegistrations() {
        return fluidRegistrar.registrations();
    }

    /**
     * 查询一种流体在 GT 流体注册表中的状态，并补齐材料关联。
     *
     * <p>解析顺序：先匹配材料的流体形态与已注册一次性流体（以补齐物态与材料键），
     * 再退回 GT 注册表直查。查询接受 {@code namespace:path}、裸 path 或已登记的资源位置。</p>
     */
    public GtFluidStatus fluidStatus(String fluidId) {
        String raw = fluidId == null ? "" : fluidId.trim();

        for (MaterialRegistration material : registrar.registrations()) {
            for (Map.Entry<String, String> fluid : material.fluids().entrySet()) {
                if (matches(fluid.getValue(), raw)
                        || matches(material.resourceLocation() + "/" + fluid.getKey(), raw)) {
                    return enrich(raw, fluid.getKey(), material.resourceLocation(), fluid.getValue());
                }
            }
        }
        for (FluidRegistration fluid : fluidRegistrar.registrations()) {
            if (matches(fluid.fluidId(), raw) || matches(fluid.resourceLocation(), raw)) {
                return enrich(raw, fluid.stateKey(), fluid.materialKey(), fluid.fluidId());
            }
        }
        return backend.fluidStatus(raw);
    }

    /** 查询材料注册记录中声明的全部流体形态的实时状态。 */
    public List<GtFluidStatus> materialFluids(String materialId) {
        return findRegisteredMaterial(materialId)
                .map(material -> {
                    List<GtFluidStatus> statuses = new ArrayList<>(material.fluids().size());
                    for (Map.Entry<String, String> fluid : material.fluids().entrySet()) {
                        statuses.add(enrich(fluid.getValue(), fluid.getKey(),
                                material.resourceLocation(), fluid.getValue()));
                    }
                    return List.copyOf(statuses);
                })
                .orElseGet(List::of);
    }

    /** 把后端注册表事实与注册记录里的材料关联合并为稳定视图。 */
    private GtFluidStatus enrich(String query, String stateKey, String materialKey, String fluidId) {
        GtFluidStatus status = backend.fluidStatus(fluidId);
        return new GtFluidStatus(query, status.adapterAvailable(), status.present(),
                status.fluidId().isEmpty() ? fluidId : status.fluidId(), stateKey, materialKey);
    }

    /** 将规范资源位置（{@code namespace:path}）与裸 path/查询串匹配。 */
    private static boolean matches(String canonical, String query) {
        if (canonical == null || query == null || query.isEmpty()) {
            return false;
        }
        if (canonical.equalsIgnoreCase(query)) {
            return true;
        }
        int colon = canonical.indexOf(':');
        String path = colon >= 0 ? canonical.substring(colon + 1) : canonical;
        if (path.equalsIgnoreCase(query)) {
            return true;
        }
        int lastSlash = path.lastIndexOf('/');
        return lastSlash >= 0 && path.substring(0, lastSlash).equalsIgnoreCase(query);
    }

    /** 按完整键、资源位置或 id 解析已登记的材料注册记录（大小写不敏感）。 */
    private Optional<MaterialRegistration> findRegisteredMaterial(String materialId) {
        Optional<MaterialRegistration> exact = registrar.registration(materialId);
        if (exact.isPresent()) {
            return exact;
        }
        if (materialId == null) {
            return Optional.empty();
        }
        String raw = materialId.trim();
        for (MaterialRegistration registration : registrar.registrations()) {
            if (matches(registration.resourceLocation(), raw) || registration.id().equalsIgnoreCase(raw)) {
                return Optional.of(registration);
            }
        }
        return Optional.empty();
    }

    /** 实时查询给定材料各声明部件在 GTCEu 中的生成状态与矿词。 */
    public List<GtPartStatus> partStatus(String materialId, Set<MaterialPart> parts) {
        Objects.requireNonNull(parts, "parts");
        return backend.partStatus(materialId, parts);
    }

    private static final class Holder {
        private static final GtAdapter INSTANCE = new GtAdapter(new GtceBackend());

        private Holder() {
        }
    }
}
