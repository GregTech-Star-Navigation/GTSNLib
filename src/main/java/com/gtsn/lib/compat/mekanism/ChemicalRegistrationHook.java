package com.gtsn.lib.compat.mekanism;

/**
 * 化学物质注册成功后的回调（#14），供附属 mod 挂接批量内容生成。
 *
 * <p>仅在 {@link ChemicalRegistrar} 经 sink 成功注册后回调；注册失败时不会触发。</p>
 */
@FunctionalInterface
public interface ChemicalRegistrationHook {

    /** 化学物质注册成功后回调。 */
    void onRegistered(ChemicalRegistration registration);
}
