package com.gtsn.lib.gt.registration;

/**
 * 通用注册成功后的回调钩子（#15）。
 *
 * <p>纯函数接口，不含任何 Minecraft / Forge / GTCEu 类型；由 {@link RegistrationRegistrar} 在
 * {@link RegistrationRegistrar.Sink} 成功返回后同步回调。</p>
 */
@FunctionalInterface
public interface RegistrationHook {

    /**
     * 注册成功后回调。
     *
     * @param kind             注册种类
     * @param resourceLocation 已注册条目的完整资源位置（{@code namespace:path}）
     */
    void onRegistered(RegistrationKind kind, String resourceLocation);
}
