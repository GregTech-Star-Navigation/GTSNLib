package com.gtsn.lib.core;

/**
 * 构建信息的纯函数格式化工具（不依赖任何 MC / Forge 类型，便于单元测试）。
 */
public final class GtsnBuildInfo {
    /** 当前库版本。 */
    public static final String VERSION = "0.1.0";

    private GtsnBuildInfo() {
    }

    /**
     * 生成 {@code /gtsnlib} 命令与启动日志共用的状态字符串。
     *
     * @param version          库版本
     * @param integrationCount 已注册的联动模块数量
     * @return 形如 {@code GTSNLib 0.1.0 | integrations: 0} 的状态字符串
     */
    public static String formatStatus(String version, int integrationCount) {
        return "GTSNLib " + version + " | integrations: " + integrationCount;
    }
}
