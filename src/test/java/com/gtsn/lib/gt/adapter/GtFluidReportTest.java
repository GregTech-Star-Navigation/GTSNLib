package com.gtsn.lib.gt.adapter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@link GtFluidReport} 输出格式验证：命令与 GameTest 共用的纯函数，无 MC / Forge / GTCEu 依赖（#13）。
 */
class GtFluidReportTest {

    @Test
    void commandLinesReportMaterialLinkedFluid() {
        GtFluidStatus status = new GtFluidStatus(
                "gtsnlib:star_alloy_gas", true, true,
                "gtsnlib:star_alloy_gas", "gas", "gtsnlib:star_alloy");

        assertEquals(List.of(
                "GT fluid | available: true | present: true",
                "fluid gtsnlib:star_alloy_gas: present @ gtsnlib:star_alloy_gas"
                        + " state=gas [material gtsnlib:star_alloy]"),
                GtFluidReport.commandLines(status));
    }

    @Test
    void commandLinesReportStandaloneFluid() {
        GtFluidStatus status = new GtFluidStatus(
                "gtsnlib:stellar_air", true, true, "gtsnlib:stellar_air", "gas", "");

        assertEquals(List.of(
                "GT fluid | available: true | present: true",
                "fluid gtsnlib:stellar_air: present @ gtsnlib:stellar_air state=gas [standalone]"),
                GtFluidReport.commandLines(status));
    }

    @Test
    void commandLinesReportAbsentFluid() {
        GtFluidStatus status = GtFluidStatus.missing("gtsnlib:nope", true, "", "", "");

        assertEquals(List.of(
                "GT fluid | available: true | present: false",
                "fluid gtsnlib:nope: absent"),
                GtFluidReport.commandLines(status));
    }

    @Test
    void materialFluidLineSummarizesForm() {
        GtFluidStatus status = new GtFluidStatus(
                "gtsnlib:star_alloy_plasma", true, true,
                "gtsnlib:star_alloy_plasma", "plasma", "gtsnlib:star_alloy");

        assertEquals("fluid plasma -> gtsnlib:star_alloy_plasma present=true",
                GtFluidReport.materialFluidLine(status));
    }
}
