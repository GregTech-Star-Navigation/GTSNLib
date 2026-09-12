package com.gtsn.lib.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 联动状态摘要的纯函数格式化工具（不依赖任何 MC / Forge 类型，便于单元测试）。
 *
 * <p>命令与启动日志共用同一份目标状态，保证两侧输出一致。</p>
 */
public final class IntegrationSummary {

    private IntegrationSummary() {
    }

    /** 单个联动目标的状态。 */
    public record TargetState(String modId, boolean present) {
    }

    /**
     * 生成 {@code /gtsnlib} 命令的多行输出：首行为总览，其后每个目标一行。
     */
    public static List<String> commandLines(String version, int registeredCount, List<TargetState> targets) {
        long present = targets.stream().filter(TargetState::present).count();
        List<String> lines = new ArrayList<>();
        lines.add(GtsnBuildInfo.formatStatus(version, registeredCount)
                + " | targets present: " + present + "/" + targets.size());
        for (TargetState target : targets) {
            lines.add(target.modId() + ": " + (target.present() ? "present" : "absent"));
        }
        return List.copyOf(lines);
    }

    /**
     * 生成启动日志摘要，例如 {@code integrations detected: 1/2 [mekanism]}。
     */
    public static String detectedLogLine(List<TargetState> targets) {
        List<String> present = targets.stream()
                .filter(TargetState::present)
                .map(TargetState::modId)
                .collect(Collectors.toList());
        String summary = "integrations detected: " + present.size() + "/" + targets.size();
        if (present.isEmpty()) {
            return summary;
        }
        return summary + " [" + String.join(", ", present) + "]";
    }
}
