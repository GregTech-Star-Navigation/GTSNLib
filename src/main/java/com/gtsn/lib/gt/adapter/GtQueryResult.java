package com.gtsn.lib.gt.adapter;

import java.util.Objects;

/**
 * 一次 GTCEu 材料查询的稳定结果视图，供命令与启动日志共用（纯数据，可单测）。
 *
 * @param query                    查询使用的材料 id
 * @param adapterAvailable         GTCEu 材料注册表是否可用
 * @param materialPresent          查询的材料是否存在
 * @param materialName             命中的材料名（缺席时为空串）
 * @param materialResourceLocation 命中的资源位置（缺席时为空串）
 * @param materialModId            命中的命名空间（缺席时为空串）
 * @param chemicalFormula          化学式（缺席或缺省时为空串）
 * @param tagPrefixCount           当前 GTCEu tag prefix 总数
 */
public record GtQueryResult(
        String query,
        boolean adapterAvailable,
        boolean materialPresent,
        String materialName,
        String materialResourceLocation,
        String materialModId,
        String chemicalFormula,
        int tagPrefixCount) {

    public GtQueryResult {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(materialName, "materialName");
        Objects.requireNonNull(materialResourceLocation, "materialResourceLocation");
        Objects.requireNonNull(materialModId, "materialModId");
        Objects.requireNonNull(chemicalFormula, "chemicalFormula");
    }

    /** 构造“命中”结果。 */
    public static GtQueryResult present(String query, boolean adapterAvailable, GtMaterialRef material,
                                        int tagPrefixCount) {
        Objects.requireNonNull(material, "material");
        return new GtQueryResult(
                query,
                adapterAvailable,
                true,
                material.name(),
                material.resourceLocation(),
                material.namespace(),
                material.chemicalFormula() == null ? "" : material.chemicalFormula(),
                tagPrefixCount);
    }

    /** 构造“缺席”结果。 */
    public static GtQueryResult missing(String query, boolean adapterAvailable, int tagPrefixCount) {
        return new GtQueryResult(query, adapterAvailable, false, "", "", "", "", tagPrefixCount);
    }
}
