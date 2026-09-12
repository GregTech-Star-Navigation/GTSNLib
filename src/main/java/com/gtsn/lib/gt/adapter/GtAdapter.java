package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.gt.registration.MaterialPart;
import com.gtsn.lib.gt.registration.MaterialRegistration;
import com.gtsn.lib.gt.registration.MaterialRegistrar;
import com.gtsn.lib.gt.registration.MaterialSpec;

import java.util.List;
import java.util.Locale;
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

    private final GtBackend backend;
    private final MaterialRegistrar registrar;

    GtAdapter(GtBackend backend) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.registrar = new MaterialRegistrar(backend::registerMaterial);
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
