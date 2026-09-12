package com.gtsn.lib.compat.mekanism;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 化学物质注册器（#14）：声明式化学注册的入口。负责重复检测与注册钩子回调，实际翻译为 Mekanism
 * 注册由注入的 {@link Sink} 完成（生产实现位于 {@code com.gtsn.lib.integration.mekanism}）。
 *
 * <p>本类不依赖任何 Minecraft / Forge / Mekanism 类型，可注入假 {@link Sink} 完整单测。</p>
 */
public final class ChemicalRegistrar {

    /** 把 {@link ChemicalSpec} 翻译为真实 Mekanism 化学注册的端口；生产实现位于隔离联动包。 */
    @FunctionalInterface
    public interface Sink {

        /**
         * 注册化学物质并返回结果视图。
         *
         * @throws RuntimeException 注册失败时向上抛出，注册器不会回调注册钩子
         */
        ChemicalRegistration register(ChemicalSpec spec);
    }

    private final Sink sink;
    private final Map<String, ChemicalRegistration> registered = new LinkedHashMap<>();

    public ChemicalRegistrar(Sink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * 注册一种化学物质：先做重复检测，再经 {@link Sink} 注册，最后回调 {@link ChemicalRegistrationHook}。
     *
     * @throws IllegalArgumentException 同一 {@code namespace:id} 已注册
     */
    public ChemicalRegistration register(ChemicalSpec spec) {
        Objects.requireNonNull(spec, "spec");
        String key = spec.key();
        if (registered.containsKey(key)) {
            throw new IllegalArgumentException("chemical already registered: " + key);
        }
        ChemicalRegistration registration = Objects.requireNonNull(sink.register(spec), "sink returned null");
        registered.put(key, registration);
        spec.registrationHook().ifPresent(hook -> hook.onRegistered(registration));
        return registration;
    }

    /** 给定 {@code namespace:id} 是否已注册。 */
    public boolean isRegistered(String key) {
        return registered.containsKey(key);
    }

    /** 按 {@code namespace:id} 查询注册结果。 */
    public Optional<ChemicalRegistration> registration(String key) {
        return Optional.ofNullable(registered.get(key));
    }

    /** 按注册顺序返回全部结果视图。 */
    public List<ChemicalRegistration> registrations() {
        return List.copyOf(registered.values());
    }
}
