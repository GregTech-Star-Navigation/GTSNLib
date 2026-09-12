package com.gtsn.lib.gt.registration;

import java.util.Objects;

/**
 * 一次机器注册的结果视图（#15，GTSN 自有类型，不含任何 GTCEu 类型），供命令、日志与钩子共用。
 *
 * @param id               机器 id 路径（已归一化，如 {@code test_machine}）
 * @param namespace        机器命名空间（如 {@code gtsnlib}）
 * @param resourceLocation 完整资源位置（{@code namespace:id}）
 * @param tier             声明的能量等级
 */
public record MachineRegistration(
        String id,
        String namespace,
        String resourceLocation,
        int tier) {

    public MachineRegistration {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(resourceLocation, "resourceLocation");
    }
}
