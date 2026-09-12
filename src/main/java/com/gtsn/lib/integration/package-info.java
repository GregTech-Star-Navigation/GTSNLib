/**
 * 联动集成层（ADR-0003）。
 *
 * <p>每个目标 mod 一个独立子包 {@code com.gtsn.lib.integration.<mod>}，内含实现
 * {@link com.gtsn.lib.api.IntegrationModule} 的骨架类。{@link com.gtsn.lib.integration.IntegrationModules}
 * 通过 {@code Supplier<Supplier<IntegrationModule>>} 双层 supplier 登记各模块——模块类名只出现在内层
 * supplier 内，目标 mod 缺席时不会被 JVM 链接，杜绝 {@code NoClassDefFoundError}。</p>
 *
 * <p>纪律：本包与门面 / 事件订阅类不得出现任何目标 mod 类型（字段、方法签名、父类、注解值）。</p>
 */
package com.gtsn.lib.integration;
