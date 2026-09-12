package com.gtsn.lib.api;

import java.util.Arrays;
import java.util.List;

/**
 * GTSNLib 已知的 6 个联动目标。
 *
 * <p>仅承载 ModID 与展示名，不含任何目标 mod 的类型。即使对应联动模块尚未实现（#5–#10），
 * 也能让 {@code /gtsnlib} 命令与启动日志报告其在场状态。</p>
 */
public enum IntegrationTargets {

    MEKANISM("mekanism", "Mekanism"),
    IMMERSIVE_ENGINEERING("immersiveengineering", "Immersive Engineering"),
    CREATE("create", "Create"),
    AE2("ae2", "Applied Energistics 2"),
    ENDER_IO("enderio", "Ender IO"),
    AD_ASTRA("ad_astra", "Ad Astra");

    private final String modId;
    private final String displayName;

    IntegrationTargets(String modId, String displayName) {
        this.modId = modId;
        this.displayName = displayName;
    }

    public String modId() {
        return modId;
    }

    public String displayName() {
        return displayName;
    }

    /** 按声明顺序返回全部目标 ModID。 */
    public static List<String> modIds() {
        return Arrays.stream(values()).map(IntegrationTargets::modId).toList();
    }
}
