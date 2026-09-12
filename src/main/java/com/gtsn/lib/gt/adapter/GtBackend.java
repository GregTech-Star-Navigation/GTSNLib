package com.gtsn.lib.gt.adapter;

import java.util.Collection;

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
}
