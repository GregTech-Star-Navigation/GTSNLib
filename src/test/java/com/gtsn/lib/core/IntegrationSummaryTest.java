package com.gtsn.lib.core;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link IntegrationSummary} 输出格式验证：命令与日志共用的纯函数，无 MC / Forge 依赖。
 */
class IntegrationSummaryTest {

    private static final List<IntegrationSummary.TargetState> TWO_TARGETS = List.of(
            new IntegrationSummary.TargetState("mekanism", true),
            new IntegrationSummary.TargetState("create", false));

    @Test
    void commandLinesListStatusAndEachTarget() {
        assertEquals(List.of(
                "GTSNLib 0.1.0 | integrations: 0 | targets present: 1/2",
                "mekanism: present",
                "create: absent"),
                IntegrationSummary.commandLines("0.1.0", 0, TWO_TARGETS));
    }

    @Test
    void commandLinesReportAllAbsentTargets() {
        List<IntegrationSummary.TargetState> states = List.of(
                new IntegrationSummary.TargetState("mekanism", false),
                new IntegrationSummary.TargetState("immersiveengineering", false),
                new IntegrationSummary.TargetState("create", false),
                new IntegrationSummary.TargetState("ae2", false),
                new IntegrationSummary.TargetState("enderio", false),
                new IntegrationSummary.TargetState("ad_astra", false));

        assertEquals(
                "GTSNLib 0.1.0 | integrations: 0 | targets present: 0/6",
                IntegrationSummary.commandLines("0.1.0", 0, states).get(0));
    }

    @Test
    void detectedLogLineListsPresentTargets() {
        assertEquals("integrations detected: 1/2 [mekanism]",
                IntegrationSummary.detectedLogLine(TWO_TARGETS));
    }

    @Test
    void detectedLogLineWithoutPresentTargets() {
        List<IntegrationSummary.TargetState> states = List.of(
                new IntegrationSummary.TargetState("mekanism", false),
                new IntegrationSummary.TargetState("create", false));

        assertEquals("integrations detected: 0/2", IntegrationSummary.detectedLogLine(states));
    }
}
