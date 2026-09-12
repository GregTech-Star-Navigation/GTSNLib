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
}
