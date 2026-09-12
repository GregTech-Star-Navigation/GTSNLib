package com.gtsn.lib.gt.adapter;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link GtFluidStatus} 的纯函数格式化工具（无 MC / Forge / GTCEu 依赖，便于单元测试），#13。
 *
 * <p>命令 {@code /gtsnlib gt fluid} 与 GameTest 共用同一份查询结果，保证输出一致。</p>
 */
public final class GtFluidReport {

    private GtFluidReport() {
    }

    /** 生成 {@code /gtsnlib gt fluid <id>} 的多行输出。 */
    public static List<String> commandLines(GtFluidStatus status) {
        List<String> lines = new ArrayList<>(2);
        lines.add("GT fluid | available: " + status.adapterAvailable()
                + " | present: " + status.present());
        if (!status.present()) {
            lines.add("fluid " + status.query() + ": absent");
            return List.copyOf(lines);
        }
        StringBuilder present = new StringBuilder("fluid ").append(status.query())
                .append(": present @ ").append(status.fluidId());
        if (!status.stateKey().isBlank()) {
            present.append(" state=").append(status.stateKey());
        }
        if (status.materialLinked()) {
            present.append(" [material ").append(status.materialKey()).append(']');
        } else {
            present.append(" [standalone]");
        }
        lines.add(present.toString());
        return List.copyOf(lines);
    }

    /** 生成材料流体形态的一行摘要，供 {@code /gtsnlib gt material} 复用。 */
    public static String materialFluidLine(GtFluidStatus status) {
        return "fluid " + status.stateKey() + " -> " + status.fluidId()
                + " present=" + status.present();
    }
}
