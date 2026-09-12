package com.gtsn.lib.core.config;

import dev.toma.configuration.Configuration;
import dev.toma.configuration.config.Config;
import dev.toma.configuration.config.Configurable;
import dev.toma.configuration.config.format.ConfigFormats;

/**
 * GTSNLib 客户端配置（client）：仅在客户端注册。
 */
@Config(id = "gtsnlib-client")
public class GtsnClientConfig {

    public static GtsnClientConfig INSTANCE;
    private static final Object LOCK = new Object();

    public static void init() {
        synchronized (LOCK) {
            if (INSTANCE == null) {
                INSTANCE = Configuration.registerConfig(GtsnClientConfig.class, ConfigFormats.json())
                        .getConfigInstance();
            }
        }
    }

    @Configurable
    @Configurable.Comment({
            "是否在联动状态命令输出中附带目标 mod 的展示名。",
            "Whether to append target display names to the /gtsnlib output.",
            "Default: true"
    })
    public boolean showTargetDisplayNames = true;
}
