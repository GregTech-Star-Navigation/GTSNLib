package com.gtsn.lib.api;

/**
 * 单个外部 mod 的联动模块契约。
 *
 * <p>实现类可以自由引用目标 mod 的类型（字段、方法签名）；但只允许在目标 mod 确认在场后，
 * 经 {@code IntegrationRegistry} 以 {@code Supplier<Supplier<IntegrationModule>>} 延迟实例化，
 * 以满足 ADR-0003 的加载隔离纪律。</p>
 */
public interface IntegrationModule {

    /** 目标 mod 的 ModID，例如 {@code mekanism}。 */
    String modId();

    /** 目标 mod 是否在场（模块自身的判定，供直接使用场景）。 */
    boolean isPresent();

    /** 在目标 mod 在场时执行联动初始化。 */
    void init();

    /** 供日志与命令展示的名称，默认回退到 ModID。 */
    default String displayName() {
        return modId();
    }
}
