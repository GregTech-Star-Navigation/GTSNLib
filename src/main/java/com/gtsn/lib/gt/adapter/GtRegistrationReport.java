package com.gtsn.lib.gt.adapter;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link GtContentStatus} 的纯函数格式化工具（#15，无 MC / Forge / GTCEu 依赖，便于单元测试）。
 *
 * <p>命令 {@code /gtsnlib reg} 与 GameTest 共用同一份结果，保证输出一致。</p>
 */
public final class GtRegistrationReport {

    private GtRegistrationReport() {
    }

    /** 生成 {@code /gtsnlib reg} 的汇总行。 */
    public static String headerLine(int blocks, int items, int machines) {
        return "GTSNLib registrations | blocks: " + blocks + " | items: " + items + " | machines: " + machines;
    }

    /** 生成单条注册的存在性行。 */
    public static String line(GtContentStatus status) {
        String prefix = status.kind().key() + " " + status.query() + ": ";
        if (!status.present()) {
            return prefix + "absent";
        }
        return prefix + "present=true @ " + status.resourceLocation();
    }

    /** 生成 {@code /gtsnlib reg} 的完整输出：汇总行 + 每条注册一行。 */
    public static List<String> commandLines(List<GtContentStatus> statuses, int blocks, int items, int machines) {
        List<String> lines = new ArrayList<>(statuses.size() + 1);
        lines.add(headerLine(blocks, items, machines));
        for (GtContentStatus status : statuses) {
            lines.add(line(status));
        }
        return List.copyOf(lines);
    }
}
