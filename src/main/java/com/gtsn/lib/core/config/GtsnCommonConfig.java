package com.gtsn.lib.core.config;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

/**
 * GTSNLib 通用配置（common）：客户端与服务端共享。
 *
 * <p>采用 GTCEu 同款 {@code dev.toma.configuration} 框架，配置项以字段声明，注册后即可读写。</p>
 */
@Config(id = "gtsnlib")
public class GtsnCommonConfig {

    public static GtsnCommonConfig INSTANCE;
    private static final Object LOCK = new Object();

    public static void init() {
        synchronized (LOCK) {
            if (INSTANCE == null) {
                INSTANCE = Configuration.registerConfig(GtsnCommonConfig.class, ConfigFormats.json())
                        .getConfigInstance();
            }
        }
    }

    @Configurable
    @Configurable.Comment({
            "是否在启动日志中输出联动检测摘要。",
            "Whether to log the integration detection summary at startup.",
            "Default: true"
    })
    public boolean logIntegrationSummary = true;

    @Configurable
    @Configurable.Comment({
            "是否登记 GTSNLib 演示内容（材料/流体/方块/物品/机器/化学物质）。",
            "生产安装默认 false：前置库不应把演示内容写进每个安装的注册表与世界。",
            "开发运行（runClient/runServer/runGameTestServer）恒登记，此项不影响。",
            "Whether to register GTSNLib demo content (material/fluids/block/item/machine/chemical).",
            "Default false in production so a prerequisite library does not pollute every install;",
            "dev runs always register demo content regardless of this option.",
            "Default: false"
    })
    public boolean registerDemoContent = false;

    /** 配置是否显式选择加入演示内容登记（未初始化配置时视为未选择加入）。 */
    public static boolean demoContentOptedIn() {
        return INSTANCE != null && INSTANCE.registerDemoContent;
    }
}
