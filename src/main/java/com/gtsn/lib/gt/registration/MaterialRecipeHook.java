package com.gtsn.lib.gt.registration;

/**
 * 材料注册后的配方钩子：在材料成功注册（衍生件与矿词已生成）之后回调，供附属 mod 批量产出配方。
 *
 * <p>这是声明式材料注册的扩展点；GTSNLib 本票只提供接缝，不在钩子内实现任何具体配方。
 * 钩子只接收 {@link MaterialRegistration} 这一 GTSN 自有视图，因此不强迫钩子实现者接触 GTCEu 内部。</p>
 */
@FunctionalInterface
public interface MaterialRecipeHook {

    /**
     * 材料注册成功后调用。
     *
     * @param registration 已生成材料的结果视图（id、资源位置、衍生件、矿词）
     */
    void onRegistered(MaterialRegistration registration);
}
