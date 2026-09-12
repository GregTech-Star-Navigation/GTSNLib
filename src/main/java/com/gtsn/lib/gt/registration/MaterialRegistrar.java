package com.gtsn.lib.gt.registration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 材料注册器：声明式材料注册的入口。负责重复检测与配方钩子回调，实际翻译为 GTCEu 注册由注入的
 * {@link Sink} 完成（生产实现位于 {@link com.gtsn.lib.gt.adapter}）。
 *
 * <p>本类不依赖任何 Minecraft / Forge / GTCEu 类型，可注入假 {@link Sink} 完整单测。</p>
 */
public final class MaterialRegistrar {

    /** 把 {@link MaterialSpec} 翻译为真实 GTCEu 注册的端口；生产实现位于适配层。 */
    @FunctionalInterface
    public interface Sink {

        /**
         * 注册材料并返回结果视图。
         *
         * @throws RuntimeException 注册失败时向上抛出，注册器不会回调配方钩子
         */
        MaterialRegistration register(MaterialSpec spec);
    }

    private final Sink sink;
    private final Map<String, MaterialRegistration> registered = new LinkedHashMap<>();

    public MaterialRegistrar(Sink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * 注册一种材料：先做重复检测，再经 {@link Sink} 注册，最后回调 {@link MaterialRecipeHook}。
     *
     * @throws IllegalArgumentException 同一 {@code namespace:id} 已注册
     */
    public MaterialRegistration register(MaterialSpec spec) {
        Objects.requireNonNull(spec, "spec");
        String key = spec.key();
        if (registered.containsKey(key)) {
            throw new IllegalArgumentException("material already registered: " + key);
        }
        MaterialRegistration registration = Objects.requireNonNull(sink.register(spec), "sink returned null");
        registered.put(key, registration);
        spec.recipeHook().ifPresent(hook -> hook.onRegistered(registration));
        return registration;
    }

    /** 给定 {@code namespace:id} 是否已注册。 */
    public boolean isRegistered(String key) {
        return registered.containsKey(key);
    }

    /** 按 {@code namespace:id} 查询注册结果。 */
    public Optional<MaterialRegistration> registration(String key) {
        return Optional.ofNullable(registered.get(key));
    }

    /** 按注册顺序返回全部结果视图。 */
    public List<MaterialRegistration> registrations() {
        return List.copyOf(registered.values());
    }
}
