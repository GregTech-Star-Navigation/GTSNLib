package com.gtsn.lib.core.config;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

/**
 * GTSNLib 服务端配置（server）：仅在专职服务端注册。
 */
@Config(id = "gtsnlib-server")
public class GtsnServerConfig {

    public static GtsnServerConfig INSTANCE;
    private static final Object LOCK = new Object();

    public static void init() {
        synchronized (LOCK) {
            if (INSTANCE == null) {
                INSTANCE = Configuration.registerConfig(GtsnServerConfig.class, ConfigFormats.json())
                        .getConfigInstance();
            }
        }
    }

    @Configurable
    @Configurable.Comment({
            "单个联动模块初始化失败的日志级别：true 记为错误，false 记为警告。",
            "Whether failed integration initialization is logged at ERROR (true) or WARN (false).",
            "Default: true"
    })
    public boolean logIntegrationFailuresAsError = true;
}
