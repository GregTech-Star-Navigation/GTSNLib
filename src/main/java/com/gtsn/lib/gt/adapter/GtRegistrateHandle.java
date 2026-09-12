package com.gtsn.lib.gt.adapter;

import com.gregtechceu.gtceu.api.registry.registrate.GTRegistrate;

import java.util.Objects;

/**
 * GT 注册入口句柄：库内对 GTCEu {@code GTRegistrate} 的稳定包装。
 *
 * <p>这是 #13（流体 / 气体 / 等离子体注册）将使用的注册入口。材料注册（#12）已落地，经
 * {@link GtAdapter#registerMaterial} 翻译并由 {@code GTCEuAPI.materialManager} 提供材料注册表，
 * 因此本类当前服务于后续流体类注册。本类只提供入口访问，**不实现**注册便捷 DSL；后续票在同一适配层内
 * 为本类补充注册方法，从而保证 GTCEu 类型始终不越过 {@code com.gtsn.lib.gt.adapter} 边界。</p>
 *
 * <p>上游迁移点（ADR-0005）：GTCEu 8.0 移除 {@code GTRegistrate#registerRegistrate}，
 * 届时只需改本类的包装实现。</p>
 */
public final class GtRegistrateHandle {

    private final String modId;
    private final GTRegistrate registrate;

    private GtRegistrateHandle(String modId, GTRegistrate registrate) {
        this.modId = modId;
        this.registrate = registrate;
    }

    /** 通过 GTCEu 7.5.3 的 {@code GTRegistrate.create(String)} 建立注册入口。 */
    static GtRegistrateHandle create(String modId) {
        Objects.requireNonNull(modId, "modId");
        return new GtRegistrateHandle(modId, GTRegistrate.create(modId));
    }

    /** 该注册入口所属的 ModID。 */
    public String modId() {
        return modId;
    }

    /**
     * 原始 GT 注册入口，供适配层内的后续注册票（#12/#13）使用。
     * 有意保持包内可见：GTCEu 类型不得越过适配层边界。
     */
    GTRegistrate unwrap() {
        return registrate;
    }
}
