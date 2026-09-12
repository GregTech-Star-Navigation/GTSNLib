package com.gtsn.lib.gt.registration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 流体注册器（#13）：声明式流体注册的入口。负责重复检测与注册钩子回调，实际翻译为 GTCEu 注册
 * 由注入的 {@link Sink} 完成（生产实现位于 {@link com.gtsn.lib.gt.adapter}）。
 *
 * <p>本类不依赖任何 Minecraft / Forge / GTCEu 类型，可注入假 {@link Sink} 完整单测。</p>
 */
public final class FluidRegistrar {

    /** 把 {@link FluidSpec} 翻译为真实 GTCEu 流体注册的端口；生产实现位于适配层。 */
    @FunctionalInterface
    public interface Sink {

        /**
         * 注册流体并返回结果视图。
         *
         * @throws RuntimeException 注册失败时向上抛出，注册器不会回调注册钩子
         */
        FluidRegistration register(FluidSpec spec);
    }

    private final Sink sink;
    private final Map<String, FluidRegistration> registered = new LinkedHashMap<>();

    public FluidRegistrar(Sink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * 注册一种流体：先做重复检测，再经 {@link Sink} 注册，最后回调 {@link FluidRegistrationHook}。
     *
     * @throws IllegalArgumentException 同一 {@code namespace:id} 已注册
     */
    public FluidRegistration register(FluidSpec spec) {
        Objects.requireNonNull(spec, "spec");
        String key = spec.key();
        if (registered.containsKey(key)) {
            throw new IllegalArgumentException("fluid already registered: " + key);
        }
        FluidRegistration registration = Objects.requireNonNull(sink.register(spec), "sink returned null");
        registered.put(key, registration);
        spec.registrationHook().ifPresent(hook -> hook.onRegistered(registration));
        return registration;
    }

    /** 给定 {@code namespace:id} 是否已注册。 */
    public boolean isRegistered(String key) {
        return registered.containsKey(key);
    }

    /** 按 {@code namespace:id} 查询注册结果。 */
    public Optional<FluidRegistration> registration(String key) {
        return Optional.ofNullable(registered.get(key));
    }

    /** 按注册顺序返回全部结果视图。 */
    public List<FluidRegistration> registrations() {
        return List.copyOf(registered.values());
    }
}
