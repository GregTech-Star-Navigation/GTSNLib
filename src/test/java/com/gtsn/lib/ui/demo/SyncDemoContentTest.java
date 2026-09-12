package com.gtsn.lib.ui.demo;

import com.gtsn.lib.ui.sync.MenuSync;
import com.gtsn.lib.ui.sync.TestIntStore;
import com.gtsn.lib.ui.sync.bind.SyncBindingGroup;
import com.gtsn.lib.ui.widget.PlainTextMetrics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 演示界面装配 + 绑定接线单测：各类型槽位是否真的接到对应控件，且随数据槽变化更新。
 */
class SyncDemoContentTest {

    @Test
    void bindsAllDemoWidgetsAndFollowsDataSlotChanges() {
        SyncDemoContent content = SyncDemoContent.build(PlainTextMetrics.INSTANCE);
        TestIntStore store = new TestIntStore(DemoSync.LAYOUT.slotCount());
        MenuSync sync = MenuSync.attach(DemoSync.LAYOUT, store);
        SyncBindingGroup group = content.bind(sync);

        try {
            assertEquals(6, group.size());
            assertEquals(0.0, content.progressBar().value(), 1e-9);
            assertTrue(content.progressLabel().text().contains("0"), content.progressLabel().text());
            assertTrue(content.phaseLabel().text().contains("IDLE"), content.phaseLabel().text());
            assertTrue(content.tierLabel().text().contains("1"), content.tierLabel().text());
            assertTrue(content.activeLabel().text().contains("是"), content.activeLabel().text());

            store.set(DemoSync.PROGRESS.index(), 42); // 模拟客户端数据槽收到服务端值
            store.set(DemoSync.PHASE.index(), DemoPhase.DONE.ordinal());
            store.set(DemoSync.TIER.index(), 3);
            store.set(DemoSync.ACTIVE.index(), 0);
            assertEquals(4, group.refresh());

            assertEquals(42.0, content.progressBar().value(), 1e-9);
            assertTrue(content.progressLabel().text().contains("42"), content.progressLabel().text());
            assertTrue(content.phaseLabel().text().contains("DONE"), content.phaseLabel().text());
            assertTrue(content.tierLabel().text().contains("4"), content.tierLabel().text());
            assertTrue(content.activeLabel().text().contains("否"), content.activeLabel().text());
        } finally {
            group.close();
        }
    }
}
