package com.gtsn.lib.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * GtsnBuildInfo.formatStatus 的行为验证（外部可观察字符串，不耦合实现）。
 */
class GtsnBuildInfoTest {

    @Test
    void formatStatusWithZeroIntegrations() {
        assertEquals("GTSNLib 0.1.0 | integrations: 0", GtsnBuildInfo.formatStatus("0.1.0", 0));
    }

    @Test
    void formatStatusReflectsIntegrationCount() {
        assertEquals("GTSNLib 0.1.0 | integrations: 3", GtsnBuildInfo.formatStatus("0.1.0", 3));
    }

    @Test
    void formatStatusUsesProvidedVersion() {
        assertEquals("GTSNLib 9.9.9 | integrations: 6", GtsnBuildInfo.formatStatus("9.9.9", 6));
    }
}
