package com.gtsn.lib.gt.adapter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link GtAdapterReport} 输出格式验证：命令与启动日志共用的纯函数，无 MC / Forge / GTCEu 依赖。
 */
class GtAdapterReportTest {

    private static final GtMaterialRef IRON =
            new GtMaterialRef("iron", "gtceu", "iron", "gtceu:iron", "material.gtceu.iron", "Fe");

    @Test
    void commandLinesReportAvailabilityAndPresentMaterial() {
        GtQueryResult present = GtQueryResult.present("iron", true, IRON, 47);

        assertEquals(List.of(
                "GT adapter | available: true | tag prefixes: 47",
                "material iron: present @ gtceu:iron [gtceu] formula=Fe"),
                GtAdapterReport.commandLines(present));
    }

    @Test
    void commandLinesReportAbsentMaterial() {
        GtQueryResult missing = GtQueryResult.missing("gold", true, 47);

        assertEquals(List.of(
                "GT adapter | available: true | tag prefixes: 47",
                "material gold: absent"),
                GtAdapterReport.commandLines(missing));
    }

    @Test
    void detectedLogLineReportsProbeResult() {
        GtQueryResult present = GtQueryResult.present("iron", true, IRON, 47);

        assertEquals("GT adapter probe: material iron present @ gtceu:iron | tag prefixes: 47",
                GtAdapterReport.detectedLogLine(present));
    }

    @Test
    void detectedLogLineReportsMissingProbe() {
        GtQueryResult missing = GtQueryResult.missing("gold", true, 47);

        assertEquals("GT adapter probe: material gold absent | tag prefixes: 47",
                GtAdapterReport.detectedLogLine(missing));
    }
}
