package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.gt.registration.FluidRegistration;
import com.gtsn.lib.gt.registration.FluidSpec;
import com.gtsn.lib.gt.registration.MaterialPart;
import com.gtsn.lib.gt.registration.MaterialRegistration;
import com.gtsn.lib.gt.registration.MaterialSpec;
import com.gtsn.lib.gt.registration.RegistrationKind;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 适配层后端：库内可注入的 GTCEu 访问端口。
 *
 * <p>仅由适配层内部使用。生产实现 {@link GtceBackend} 是**唯一**接触 GTCEu 类型的地方；
 * 单元测试可注入只含纯数据的假实现，无需加载游戏。</p>
 */
interface GtBackend {

    /** GTCEu 材料注册表是否已就绪。 */
    boolean available();

    /** 当前已注册的全部材料视图。 */
    Collection<GtMaterialRef> materials();

    /** 当前 GTCEu 已登记的全部 tag prefix 视图。 */
    Collection<GtTagPrefixRef> tagPrefixes();

    /**
     * 为给定 ModID 获取 GT 注册入口。生产实现取该命名空间材料注册表自带的 registrate
     * （{@code GTCEuAPI.materialManager.getRegistry(modId).getRegistrate()}），该实例已由 GTCEu
     * {@code CommonProxy#init()} 挂接 {@code RegisterEvent}，因此其条目会被真正注册。
     */
    GtRegistrateHandle registrate(String modId);

    /**
     * 在 GTCEu {@code MaterialRegistryEvent} 期间为给定 ModID 建立命名空间材料注册表，并返回其 registrate。
     *
     * <p>GTCEu 仅在 registry manager 的 {@code Phase.PRE}（即 {@code MaterialRegistryEvent}）允许
     * {@code createRegistry}；这是附属 mod 使用任何注册 helper 之前的前置条件。详见 {@code docs/registration.md}。</p>
     *
     * @throws IllegalStateException registry manager 未就绪、或非 PRE 阶段 / 注册表已存在时
     */
    GtRegistrateHandle createRegistrate(String modId);

    /** 将声明式材料翻译并注册进 GTCEu，返回结果视图（材料注册简化层的翻译点）。 */
    MaterialRegistration registerMaterial(MaterialSpec spec);

    /** 将声明式流体翻译并注册进 GTCEu，返回结果视图（流体注册简化层的翻译点，#13）。 */
    FluidRegistration registerFluid(FluidSpec spec);

    /**
     * 实时查询 GT 流体注册表中给定资源位置的物态与存在性（#13）。
     *
     * <p>返回视图的材料关联由门面 {@link GtAdapter} 依据注册记录补齐；后端只负责注册表事实。</p>
     *
     * @param fluidId 流体资源位置（{@code namespace:path}）
     */
    GtFluidStatus fluidStatus(String fluidId);

    /**
     * 实时查询给定材料各声明部件在 GTCEu 中的生成状态与矿词。
     *
     * @param materialId 材料资源位置（{@code namespace:path}）或裸材料名
     * @param parts      待查询的部件集合
     * @return 逐部件状态；材料不存在时为空列表
     */
    List<GtPartStatus> partStatus(String materialId, Set<MaterialPart> parts);

    /**
     * 实时查询通用注册条目（方块 / 物品 / 机器）在真实注册表中的存在性（#15）。
     *
     * @param kind 注册种类
     * @param id   资源位置（{@code namespace:path}）或裸路径（默认补全 GTSNLib 命名空间）
     */
    GtContentStatus contentStatus(RegistrationKind kind, String id);
}
