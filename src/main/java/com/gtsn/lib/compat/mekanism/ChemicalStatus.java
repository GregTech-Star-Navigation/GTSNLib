package com.gtsn.lib.compat.mekanism;

import java.util.Objects;

/**
 * 一次化学物质查询的稳定结果视图（GTSN 自有类型，不含 Mekanism 类型），供命令与 GameTest 共用（#14）。
 *
 * @param query            查询使用的 id
 * @param backendAvailable 化学注册后端是否已安装（即 Mekanism 联动是否已接线）
 * @param present          化学物质是否已存在于目标注册表
 * @param kindKey          命中的化学种类键；未知时为空串
 * @param registryId       命中的注册表资源位置；未知时为空串
 * @param resourceLocation 命中的真实资源位置；缺席时为空串
 * @param tint             命中条目的颜色；缺席时为 0
 * @param hidden           命中条目是否隐藏；缺席时为 false
 */
public record ChemicalStatus(
        String query,
        boolean backendAvailable,
        boolean present,
        String kindKey,
        String registryId,
        String resourceLocation,
        int tint,
        boolean hidden) {

    public ChemicalStatus {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(kindKey, "kindKey");
        Objects.requireNonNull(registryId, "registryId");
        Objects.requireNonNull(resourceLocation, "resourceLocation");
    }

    /** 后端未安装（Mekanism 缺席）时的结果。 */
    public static ChemicalStatus unavailable(String query) {
        return new ChemicalStatus(query, false, false, "", "", "", 0, false);
    }

    /** 构造“缺席”结果（后端可用但注册表无此条目）。 */
    public static ChemicalStatus missing(String query, boolean backendAvailable) {
        return new ChemicalStatus(query, backendAvailable, false, "", "", "", 0, false);
    }

    /** 构造带种类信息的“缺席”结果（已声明但注册表暂未填充）。 */
    public static ChemicalStatus missing(String query, boolean backendAvailable, String kindKey, String registryId) {
        return new ChemicalStatus(query, backendAvailable, false, kindKey, registryId, "", 0, false);
    }

    /** 构造“命中”结果。 */
    public static ChemicalStatus present(String query, boolean backendAvailable, String kindKey, String registryId,
                                         String resourceLocation, int tint, boolean hidden) {
        return new ChemicalStatus(query, backendAvailable, true, kindKey, registryId, resourceLocation, tint, hidden);
    }
}
