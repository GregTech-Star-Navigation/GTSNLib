package com.gtsn.lib.compat.mekanism;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Mekanism 化学注册门面（#14）：库内声明与查询 Mekanism 化学物质的唯一入口。
 *
 * <p><b>访问纪律</b>：本类只暴露 GTSN 自有类型（{@link ChemicalSpec}/{@link ChemicalRegistration}/
 * {@link ChemicalStatus}），不引用任何 Mekanism 类型；一切 Mekanism 访问经 {@link ChemicalBackend} 端口，
 * 生产实现由隔离联动包在 Mekanism 确认在场后安装。Mekanism 缺席时 {@link #available()} 为 {@code false}，
 * 不会触发任何 Mekanism 类链接。</p>
 *
 * <p>安装必须发生在 Forge {@code RegisterEvent} 触发之前（即 {@code @Mod} 构造期），因为 Mekanism 的
 * 化学注册表经 Forge 的 {@code DeferredRegister} 在注册事件期间填充。生产接线见
 * {@code com.gtsn.lib.integration.mekanism.MekanismChemicalRegistration}。</p>
 */
public final class MekanismChemicals {

    /** 游戏内证明用演示化学物质路径（#14）。 */
    public static final String DEMO_CHEMICAL_ID = "test_chemical";

    /** 演示化学物质完整键 {@code gtsnlib:test_chemical}。 */
    public static final String DEMO_CHEMICAL = "gtsnlib:" + DEMO_CHEMICAL_ID;

    private static volatile ChemicalBackend backend;
    private static volatile ChemicalRegistrar registrar;

    private MekanismChemicals() {
    }

    /**
     * 安装化学注册后端（由隔离联动包在 Mekanism 在场时调用）。重复安装会被拒绝，避免静默覆盖。
     *
     * @throws IllegalStateException 后端已安装
     */
    public static synchronized void install(ChemicalBackend chemicalBackend) {
        Objects.requireNonNull(chemicalBackend, "chemicalBackend");
        if (backend != null) {
            throw new IllegalStateException("Mekanism chemical backend is already installed");
        }
        registrar = new ChemicalRegistrar(chemicalBackend::register);
        backend = chemicalBackend;
    }

    /** 清空后端与注册记录（供单测隔离；生产不使用）。 */
    public static synchronized void reset() {
        backend = null;
        registrar = null;
    }

    /** 化学注册后端是否已安装（即 Mekanism 联动是否已接线）。 */
    public static boolean available() {
        return backend != null;
    }

    /**
     * 注册一种声明式化学物质：翻译为 Mekanism 注册、记录结果并回调注册钩子。
     *
     * @throws IllegalStateException 后端未安装（Mekanism 缺席）
     */
    public static ChemicalRegistration register(ChemicalSpec spec) {
        return requireRegistrar().register(spec);
    }

    /** 给定 {@code namespace:id} 是否已经门面注册。 */
    public static boolean isRegistered(String key) {
        ChemicalRegistrar current = registrar;
        return current != null && current.isRegistered(key);
    }

    /** 按 {@code namespace:id} 查询已注册化学物质的结果视图。 */
    public static Optional<ChemicalRegistration> registration(String key) {
        ChemicalRegistrar current = registrar;
        return current == null ? Optional.empty() : current.registration(key);
    }

    /** 已注册化学物质的只读快照（按注册顺序）。 */
    public static List<ChemicalRegistration> registrations() {
        ChemicalRegistrar current = registrar;
        return current == null ? List.of() : current.registrations();
    }

    /**
     * 查询一种化学物质在 Mekanism 注册表中的状态。
     *
     * <p>后端缺席时返回“后端不可用”的稳定视图，绝不接触 Mekanism 类型。</p>
     */
    public static ChemicalStatus status(String id) {
        String query = id == null ? "" : id.trim();
        ChemicalBackend current = backend;
        if (current == null) {
            return ChemicalStatus.unavailable(query);
        }
        return current.status(query);
    }

    private static ChemicalRegistrar requireRegistrar() {
        ChemicalRegistrar current = registrar;
        if (current == null) {
            throw new IllegalStateException("Mekanism chemical backend is not installed; is Mekanism present?");
        }
        return current;
    }
}
