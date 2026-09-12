package com.gtsn.lib.core;

import com.gtsn.lib.api.IntegrationRegistry;
import com.gtsn.lib.api.IntegrationTargets;
import com.gtsn.lib.api.ModPresence;
import com.gtsn.lib.core.IntegrationSummary.TargetState;
import com.gtsn.lib.core.config.GtsnCommonConfig;
import com.gtsn.lib.core.config.GtsnServerConfig;
import com.gtsn.lib.integration.IntegrationModules;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.List;
import java.util.Objects;

/**
 * 联动门面：创建并持有唯一的 {@link IntegrationRegistry}，登记 6 个已知目标，并在启动时初始化。
 *
 * <p>按 ADR-0003，本类不出现任何可选 mod 类型；未来 #5–#10 的联动模块只能经
 * {@code registry().register(modId, () -> () -> new XxxIntegration())} 双层 supplier 登记。</p>
 */
public final class GtsnIntegrations {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static volatile IntegrationRegistry registry;

    private GtsnIntegrations() {
    }

    /** 在 {@code @Mod} 构造期建立注册表并登记已知目标（幂等）。 */
    public static synchronized IntegrationRegistry bootstrap(ModPresence presence) {
        if (registry == null) {
            IntegrationRegistry created = new IntegrationRegistry(Objects.requireNonNull(presence, "presence"));
            created.addTargets(IntegrationTargets.modIds());
            // 经 com.gtsn.lib.integration 以双层 supplier 登记 #5–#10 的隔离模块（ADR-0003）。
            IntegrationModules.registerAll(created);
            registry = created;
        }
        return registry;
    }

    /** 返回已建立的注册表；若尚未建立则以默认 Forge 判定兜底。 */
    public static IntegrationRegistry registry() {
        IntegrationRegistry current = registry;
        return current != null ? current : bootstrap(new ForgeModPresence());
    }

    /** 实例化并初始化所有在场的联动模块，并输出启动摘要（受 common 配置开关控制）。 */
    public static IntegrationRegistry initialize() {
        IntegrationRegistry current = registry();
        current.initializeAll();
        current.failures().forEach((modId, failure) -> {
            if (GtsnServerConfig.INSTANCE != null && !GtsnServerConfig.INSTANCE.logIntegrationFailuresAsError) {
                LOGGER.warn("[GTSNLib] integration {} failed to initialize", modId, failure);
            } else {
                LOGGER.error("[GTSNLib] integration {} failed to initialize", modId, failure);
            }
        });
        if (GtsnCommonConfig.INSTANCE == null || GtsnCommonConfig.INSTANCE.logIntegrationSummary) {
            LOGGER.info("[GTSNLib] {}", IntegrationSummary.detectedLogLine(targetStates(current)));
        }
        return current;
    }

    /** 快照注册表当前的目标在场状态，供命令与日志共用。 */
    public static List<TargetState> targetStates(IntegrationRegistry current) {
        return current.targets().stream()
                .map(modId -> new TargetState(modId, current.isPresent(modId)))
                .toList();
    }
}
