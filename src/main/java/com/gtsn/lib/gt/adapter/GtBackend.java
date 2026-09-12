package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.gt.registration.MaterialPart;
import com.gtsn.lib.gt.registration.MaterialRegistration;
import com.gtsn.lib.gt.registration.MaterialSpec;

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

    /** 为给定 ModID 创建 GT 注册入口（{@code GTRegistrate.create}）。 */
    GtRegistrateHandle registrate(String modId);

    /** 将声明式材料翻译并注册进 GTCEu，返回结果视图（材料注册简化层的翻译点）。 */
    MaterialRegistration registerMaterial(MaterialSpec spec);

    /**
     * 实时查询给定材料各声明部件在 GTCEu 中的生成状态与矿词。
     *
     * @param materialId 材料资源位置（{@code namespace:path}）或裸材料名
     * @param parts      待查询的部件集合
     * @return 逐部件状态；材料不存在时为空列表
     */
    List<GtPartStatus> partStatus(String materialId, Set<MaterialPart> parts);
}
