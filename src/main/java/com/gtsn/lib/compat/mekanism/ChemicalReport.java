package com.gtsn.lib.compat.mekanism;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ChemicalStatus} 的纯函数格式化工具（无 MC / Forge / Mekanism 依赖，便于单元测试），#14。
 *
 * <p>命令 {@code /gtsnlib mek} 与 GameTest 共用同一份查询结果，保证输出一致。</p>
 */
public final class ChemicalReport {

    private ChemicalReport() {
    }

    /** 生成 {@code /gtsnlib mek} 的摘要输出。 */
    public static List<String> summaryLines(boolean backendAvailable, int registered) {
        return List.of("Mekanism chemicals | backend: " + backendAvailable + " | registered: " + registered);
    }

    /** 生成 {@code /gtsnlib mek chemical <id>} 的多行输出。 */
    public static List<String> commandLines(ChemicalStatus status) {
        List<String> lines = new ArrayList<>(2);
        lines.add("Mekanism chemical | backend: " + status.backendAvailable()
                + " | present: " + status.present());
        if (!status.present()) {
            lines.add("chemical " + status.query() + ": "
                    + (status.backendAvailable() ? "absent" : "backend unavailable"));
            return List.copyOf(lines);
        }
        StringBuilder present = new StringBuilder("chemical ").append(status.query())
                .append(": present @ ").append(status.resourceLocation())
                .append(" kind=").append(status.kindKey())
                .append(" registry=").append(status.registryId())
                .append(" tint=").append(String.format("0x%06X", status.tint()));
        if (status.hidden()) {
            present.append(" hidden");
        }
        lines.add(present.toString());
        return List.copyOf(lines);
    }
}
