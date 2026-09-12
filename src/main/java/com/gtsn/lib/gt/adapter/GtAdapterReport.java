package com.gtsn.lib.gt.adapter;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link GtQueryResult} 的纯函数格式化工具（无 MC / Forge / GTCEu 依赖，便于单元测试）。
 *
 * <p>命令 {@code /gtsnlib gt} 与启动探针日志共用同一份查询结果，保证两侧输出一致。</p>
 */
public final class GtAdapterReport {

    private GtAdapterReport() {
    }

    /** 生成 {@code /gtsnlib gt} 的多行输出。 */
    public static List<String> commandLines(GtQueryResult result) {
        List<String> lines = new ArrayList<>(2);
        lines.add("GT adapter | available: " + result.adapterAvailable()
                + " | tag prefixes: " + result.tagPrefixCount());
        if (!result.materialPresent()) {
            lines.add("material " + result.query() + ": absent");
            return List.copyOf(lines);
        }
        StringBuilder present = new StringBuilder()
                .append("material ").append(result.query())
                .append(": present @ ").append(result.materialResourceLocation());
        if (!result.materialModId().isBlank()) {
            present.append(" [").append(result.materialModId()).append(']');
        }
        if (!result.chemicalFormula().isBlank()) {
            present.append(" formula=").append(result.chemicalFormula());
        }
        lines.add(present.toString());
        return List.copyOf(lines);
    }

    /** 生成启动探针日志行，作为适配层在真实运行环境中的证据。 */
    public static String detectedLogLine(GtQueryResult result) {
        String state = result.materialPresent()
                ? "present @ " + result.materialResourceLocation()
                : "absent";
        return "GT adapter probe: material " + result.query() + " " + state
                + " | tag prefixes: " + result.tagPrefixCount();
    }
}
