package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.gt.registration.RegistrationKind;

import java.util.Objects;

/**
 * 一次通用注册条目在真实注册表中的存在性视图（#15，GTSN 自有类型，不含 GTCEu 类型），供命令与
 * GameTest 共用。
 *
 * @param kind             注册种类
 * @param query            查询使用资源位置
 * @param resourceLocation 命中的真实资源位置（缺席时为空串）
 * @param adapterAvailable 适配层依赖的注册表是否可用
 * @param present          条目是否已存在于目标注册表
 */
public record GtContentStatus(
        RegistrationKind kind,
        String query,
        String resourceLocation,
        boolean adapterAvailable,
        boolean present) {

    public GtContentStatus {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(resourceLocation, "resourceLocation");
    }

    /** 构造“命中”结果。 */
    public static GtContentStatus present(RegistrationKind kind, String query, String resourceLocation,
                                          boolean adapterAvailable) {
        return new GtContentStatus(kind, query, resourceLocation, adapterAvailable, true);
    }

    /** 构造“缺席”结果。 */
    public static GtContentStatus missing(RegistrationKind kind, String query, boolean adapterAvailable) {
        return new GtContentStatus(kind, query, "", adapterAvailable, false);
    }
}
