package com.gtsn.lib.ui.sync.bind;

import com.gtsn.lib.ui.sync.MenuSync;
import com.gtsn.lib.ui.sync.SyncCodecs;
import com.gtsn.lib.ui.sync.SyncLayout;
import com.gtsn.lib.ui.sync.SyncSlot;
import com.gtsn.lib.ui.sync.TestIntStore;
import com.gtsn.lib.ui.widget.PlainTextMetrics;
import com.gtsn.lib.ui.widget.ProgressBarWidget;
import com.gtsn.lib.ui.widget.TextWidget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 绑定层单测：初始应用、数据槽变化经 {@link MenuSync#refresh()} 驱动控件更新、
 * 去重（值未变不重复应用）、组驱动与解绑，全部经假后端（{@link TestIntStore}）验证。
 */
class SyncBindingsTest {

    private enum Phase {
        IDLE, RUNNING
    }

    private SyncSlot<Integer> progress;
    private SyncSlot<Float> speed;
    private SyncSlot<Phase> phase;
    private TestIntStore store;
    private MenuSync sync;

    @BeforeEach
    void setUp() {
        SyncLayout.Builder builder = SyncLayout.builder();
        progress = builder.slot("progress", SyncCodecs.intRange(0, 100), 0);
        speed = builder.slot("speed", SyncCodecs.floatRange(0f, 5f), 1f);
        phase = builder.slot("phase", SyncCodecs.enums(Phase.class), Phase.IDLE);
        SyncLayout layout = builder.build();
        store = new TestIntStore(3);
        sync = MenuSync.attach(layout, store);
    }

    @Test
    void progressBarBindingAppliesInitialValueOnCreation() {
        sync.set(progress, 25);
        ProgressBarWidget bar = new ProgressBarWidget().range(0, 100);

        SyncBinding binding = SyncBindings.progressBar(sync, progress, bar);

        assertEquals(25.0, bar.value(), 1e-9);
        binding.close();
    }

    @Test
    void progressBarBindingFollowsClientSideDataSlotWrites() {
        ProgressBarWidget bar = new ProgressBarWidget().range(0, 100);
        SyncBinding binding = SyncBindings.progressBar(sync, progress, bar);

        store.set(progress.index(), 60); // 模拟客户端收到服务端数据包
        assertEquals(1, sync.refresh());

        assertEquals(60.0, bar.value(), 1e-9);
        binding.close();
    }

    @Test
    void progressBarBindingAcceptsFloatSlots() {
        sync.set(speed, 2.5f);
        ProgressBarWidget bar = new ProgressBarWidget().range(0, 5);

        SyncBinding binding = SyncBindings.progressBar(sync, speed, bar);

        assertEquals(2.5, bar.value(), 1e-6);
        binding.close();
    }

    @Test
    void labelBindingFormatsInitialValue() {
        sync.set(phase, Phase.RUNNING);
        TextWidget text = new TextWidget("", PlainTextMetrics.INSTANCE);

        SyncBinding binding = SyncBindings.label(sync, phase, text, value -> "phase=" + value);

        assertEquals("phase=RUNNING", text.text());
        binding.close();
    }

    @Test
    void labelBindingOnlyReappliesWhenValueChanged() {
        AtomicInteger formatCalls = new AtomicInteger();
        TextWidget text = new TextWidget("", PlainTextMetrics.INSTANCE);
        SyncBinding binding = SyncBindings.label(sync, progress, text,
                value -> "v=" + value + "#" + formatCalls.incrementAndGet());

        assertEquals("v=0#1", text.text());
        sync.refresh(); // 无变化
        assertEquals("v=0#1", text.text());

        store.set(progress.index(), 5);
        sync.refresh();
        assertEquals("v=5#2", text.text());
        binding.close();
    }

    @Test
    void bindingGroupDrivesAllBindingsAndReportsChanges() {
        ProgressBarWidget bar = new ProgressBarWidget().range(0, 100);
        TextWidget progressText = new TextWidget("", PlainTextMetrics.INSTANCE);
        TextWidget phaseText = new TextWidget("", PlainTextMetrics.INSTANCE);
        SyncBindingGroup group = SyncBindingGroup.of(sync);
        group.add(SyncBindings.progressBar(sync, progress, bar));
        group.add(SyncBindings.label(sync, progress, progressText, value -> value + "%"));
        group.add(SyncBindings.label(sync, phase, phaseText, value -> value.toString()));

        assertEquals(3, group.size());
        assertEquals(0, group.refresh());

        store.set(progress.index(), 80);
        store.set(phase.index(), Phase.RUNNING.ordinal());
        assertEquals(2, group.refresh());

        assertEquals(80.0, bar.value(), 1e-9);
        assertEquals("80%", progressText.text());
        assertEquals("RUNNING", phaseText.text());
        assertEquals(0, group.refresh());
        group.close();
    }

    @Test
    void closedBindingNoLongerUpdatesTarget() {
        ProgressBarWidget bar = new ProgressBarWidget().range(0, 100);
        SyncBinding binding = SyncBindings.progressBar(sync, progress, bar);
        binding.close();
        binding.close(); // 幂等

        store.set(progress.index(), 99);
        sync.refresh();

        assertEquals(0.0, bar.value(), 1e-9, "解绑后不应再更新控件");
    }

    @Test
    void bindingGroupCloseDetachesAllBindings() {
        ProgressBarWidget bar = new ProgressBarWidget().range(0, 100);
        SyncBindingGroup group = SyncBindingGroup.of(sync);
        group.add(SyncBindings.progressBar(sync, progress, bar));
        group.close();
        group.close(); // 幂等

        store.set(progress.index(), 99);
        sync.refresh();

        assertEquals(0.0, bar.value(), 1e-9);
    }

    @Test
    void bindingFactoriesRejectNullArguments() {
        ProgressBarWidget bar = new ProgressBarWidget();
        TextWidget text = new TextWidget("", PlainTextMetrics.INSTANCE);

        assertThrows(NullPointerException.class, () -> SyncBindings.progressBar(null, progress, bar));
        assertThrows(NullPointerException.class, () -> SyncBindings.progressBar(sync, null, bar));
        assertThrows(NullPointerException.class, () -> SyncBindings.progressBar(sync, progress, null));
        assertThrows(NullPointerException.class, () -> SyncBindings.label(sync, progress, text, null));
        assertThrows(NullPointerException.class, () -> SyncBindingGroup.of(null));
        assertThrows(NullPointerException.class, () -> SyncBindingGroup.of(sync).add(null));
    }
}
