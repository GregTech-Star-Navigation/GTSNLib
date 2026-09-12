package com.gtsn.lib.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

/**
 * 联动注册表：登记已知联动目标与联动模块工厂，查询在场状态，并在目标 mod 在场时延迟初始化。
 *
 * <p>本类不引用任何 Forge 或可选 mod 类型，可注入假 {@link ModPresence} 做纯单元测试。</p>
 *
 * <p>加载隔离（ADR-0003）：模块工厂采用 {@code Supplier<Supplier<IntegrationModule>>} 双层
 * supplier，只有 {@link ModPresence} 判定目标在场后才会调用外层与内层 supplier，缺席时目标类型
 * 不会被 JVM 链接。单个模块的实例化或 {@link IntegrationModule#init()} 失败会被记录到
 * {@link #failures()} 并跳过，不阻断其它模块。</p>
 */
public final class IntegrationRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();

    private final ModPresence presence;
    private final List<String> targets = new ArrayList<>();
    private final Map<String, Supplier<Supplier<IntegrationModule>>> factories = new LinkedHashMap<>();
    private final Map<String, IntegrationModule> initialized = new LinkedHashMap<>();
    private final Map<String, Throwable> failures = new LinkedHashMap<>();

    public IntegrationRegistry(ModPresence presence) {
        this.presence = Objects.requireNonNull(presence, "presence");
    }

    /** 登记一个已知联动目标（即使其模块尚未实现，也用于报告在场状态）。 */
    public void addTarget(String modId) {
        requireModId(modId);
        if (!targets.contains(modId)) {
            targets.add(modId);
        }
    }

    public void addTargets(Collection<String> modIds) {
        Objects.requireNonNull(modIds, "modIds");
        for (String modId : modIds) {
            addTarget(modId);
        }
    }

    /** 按登记顺序返回全部已知目标。 */
    public List<String> targets() {
        return Collections.unmodifiableList(targets);
    }

    public boolean isTarget(String modId) {
        return targets.contains(modId);
    }

    /**
     * 登记联动模块工厂，同时把 modId 记为已知目标。同一 modId 不可重复登记。
     *
     * @param modId   目标 mod 的 ModID
     * @param factory 双层 supplier，缺席时不会被调用
     */
    public void register(String modId, Supplier<Supplier<IntegrationModule>> factory) {
        requireModId(modId);
        Objects.requireNonNull(factory, "factory");
        if (factories.containsKey(modId)) {
            throw new IllegalArgumentException("integration already registered: " + modId);
        }
        addTarget(modId);
        factories.put(modId, factory);
    }

    public boolean isRegistered(String modId) {
        return factories.containsKey(modId);
    }

    /** 已登记的联动模块数量（不区分是否在场）。 */
    public int registeredCount() {
        return factories.size();
    }

    public boolean isPresent(String modId) {
        return presence.isLoaded(modId);
    }

    /** 返回已初始化的模块；未在场或初始化失败则为空。 */
    public Optional<IntegrationModule> get(String modId) {
        return Optional.ofNullable(initialized.get(modId));
    }

    /** 按登记顺序返回已初始化的模块。 */
    public List<IntegrationModule> modules() {
        return Collections.unmodifiableList(new ArrayList<>(initialized.values()));
    }

    /**
     * 实例化并初始化所有在场且尚未初始化的模块。单个模块失败会被记录并跳过。
     *
     * @return 本次调用新初始化的模块数量
     */
    public int initializeAll() {
        int count = 0;
        for (Map.Entry<String, Supplier<Supplier<IntegrationModule>>> entry : factories.entrySet()) {
            String modId = entry.getKey();
            if (initialized.containsKey(modId) || !presence.isLoaded(modId)) {
                continue;
            }
            try {
                IntegrationModule module = entry.getValue().get().get();
                module.init();
                initialized.put(modId, module);
                count++;
            } catch (Exception failure) {
                // 模块实例化 / 初始化抛出的常规异常：记录并跳过，不阻断其它模块。
                LOGGER.error("[GTSNLib] integration {} failed to initialize", modId, failure);
                failures.put(modId, failure);
            } catch (LinkageError failure) {
                // 目标 mod 类型缺席导致的类链接失败（NoClassDefFoundError / ExceptionInInitializerError 等）：
                // 显式记录，绝不静默吞掉真实的链接错误。
                LOGGER.error("[GTSNLib] integration {} could not be linked (missing target classes?)",
                        modId, failure);
                failures.put(modId, failure);
            } catch (Error failure) {
                // 其它 JVM 级错误（如 OOM）同样显式记录后再抛出范围外的处理。
                LOGGER.error("[GTSNLib] integration {} failed with a JVM error", modId, failure);
                failures.put(modId, failure);
            }
        }
        return count;
    }

    /** 上次初始化过程中失败的模块（modId → 失败原因）。 */
    public Map<String, Throwable> failures() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(failures));
    }

    private static void requireModId(String modId) {
        if (modId == null || modId.isBlank()) {
            throw new IllegalArgumentException("modId must not be blank");
        }
    }
}
