package com.gtsn.lib.compat.mekanism;

/**
 * 化学物质注册后端端口（#14）：库内可注入的 Mekanism 访问端口。
 *
 * <p>仅由 {@link MekanismChemicals} 门面使用。生产实现 {@code MekanismChemicalBackend} 位于隔离联动包
 * {@code com.gtsn.lib.integration.mekanism}，是**唯一**接触 Mekanism 类型的地方；单元测试可注入只含纯数据的
 * 假实现，无需加载游戏。</p>
 */
public interface ChemicalBackend {

    /** 后端是否可用（Mekanism 注册表是否已接线）。 */
    boolean available();

    /** 将声明式化学规格翻译并注册进 Mekanism，返回结果视图。 */
    ChemicalRegistration register(ChemicalSpec spec);

    /**
     * 实时查询给定 id 在 Mekanism 化学注册表中的存在性与种类。
     *
     * @param id 化学物质 id（{@code namespace:path} 或裸 path，默认补全 GTSNLib 命名空间）
     */
    ChemicalStatus status(String id);
}
