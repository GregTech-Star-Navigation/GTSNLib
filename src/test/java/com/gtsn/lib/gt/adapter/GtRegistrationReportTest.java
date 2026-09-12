package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.gt.registration.RegistrationKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link GtRegistrationReport} 的纯函数格式化验证（#15）：不依赖任何 Minecraft / Forge / GTCEu 类型。
 */
class GtRegistrationReportTest {

    @Test
    void headerLineFormatsCounts() {
        assertEquals("GTSNLib registrations | blocks: 1 | items: 2 | machines: 3",
                GtRegistrationReport.headerLine(1, 2, 3));
    }

    @Test
    void lineForPresentStatusReportsPresence() {
        GtContentStatus status = GtContentStatus.present(
                RegistrationKind.BLOCK, "gtsnlib:test_block", "gtsnlib:test_block", true);

        String line = GtRegistrationReport.line(status);

        assertTrue(line.contains("block gtsnlib:test_block"), line);
        assertTrue(line.contains("present=true"), line);
        assertTrue(line.contains("@ gtsnlib:test_block"), line);
    }

    @Test
    void lineForAbsentStatusReportsAbsence() {
        GtContentStatus status = GtContentStatus.missing(RegistrationKind.ITEM, "gtsnlib:test_item", true);

        String line = GtRegistrationReport.line(status);

        assertTrue(line.contains("item gtsnlib:test_item"), line);
        assertTrue(line.contains("absent"), line);
    }

    @Test
    void commandLinesIncludeHeaderAndEveryRegistration() {
        List<GtContentStatus> statuses = List.of(
                GtContentStatus.present(RegistrationKind.BLOCK, "gtsnlib:test_block", "gtsnlib:test_block", true),
                GtContentStatus.present(RegistrationKind.ITEM, "gtsnlib:test_item", "gtsnlib:test_item", true));

        List<String> lines = GtRegistrationReport.commandLines(statuses, 1, 1, 0);

        assertEquals(3, lines.size());
        assertEquals("GTSNLib registrations | blocks: 1 | items: 1 | machines: 0", lines.get(0));
        assertTrue(lines.get(1).contains("block gtsnlib:test_block"), lines.get(1));
        assertTrue(lines.get(2).contains("item gtsnlib:test_item"), lines.get(2));
    }
}
