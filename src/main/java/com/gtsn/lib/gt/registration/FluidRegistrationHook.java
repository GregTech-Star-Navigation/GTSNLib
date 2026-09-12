package com.gtsn.lib.gt.registration;

/**
 * 流体注册成功后的回调（#13），供附属 mod 挂接批量内容生成。
 *
 * <p>仅在 {@link FluidRegistrar} 经 sink 成功注册后回调；注册失败时不会触发。</p>
 */
@FunctionalInterface
public interface FluidRegistrationHook {

    /** 流体注册成功后回调。 */
    void onRegistered(FluidRegistration registration);
}
