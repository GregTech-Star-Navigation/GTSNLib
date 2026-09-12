package com.gtsn.lib.gt.adapter;

import java.util.Objects;

/**
 * 一次流体查询的稳定结果视图（GTSN 自有类型，不含 GTCEu 类型），供命令与日志共用（#13）。
 *
 * @param query            查询使用的流体 id
 * @param adapterAvailable GT 注册表是否可用
 * @param present          流体是否已存在于 GT 流体注册表
 * @param fluidId          命中的 GT 流体资源位置（缺席时为空串）
 * @param stateKey         物态键（{@code liquid}/{@code gas}/{@code plasma}）；未知时为空串
 * @param materialKey      关联材料的完整键；独立流体或未知时为空串
 */
public record GtFluidStatus(
        String query,
        boolean adapterAvailable,
        boolean present,
        String fluidId,
        String stateKey,
        String materialKey) {

    public GtFluidStatus {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(fluidId, "fluidId");
        Objects.requireNonNull(stateKey, "stateKey");
        Objects.requireNonNull(materialKey, "materialKey");
    }

    /** 是否为关联了材料的流体形态。 */
    public boolean materialLinked() {
        return !materialKey.isEmpty();
    }

    /** 构造“命中”结果。 */
    public static GtFluidStatus present(String query, boolean adapterAvailable, String fluidId,
                                        String stateKey, String materialKey) {
        return new GtFluidStatus(query, adapterAvailable, true, fluidId, stateKey, materialKey);
    }

    /** 构造“缺席”结果。 */
    public static GtFluidStatus missing(String query, boolean adapterAvailable, String fluidId,
                                        String stateKey, String materialKey) {
        return new GtFluidStatus(query, adapterAvailable, false, fluidId, stateKey, materialKey);
    }
}
