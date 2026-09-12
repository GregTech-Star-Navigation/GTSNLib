package com.gtsn.lib.ui.sync;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MenuSync} 单测：默认值初始化、类型化读写、外部直写（模拟客户端收包）检测、
 * 监听器通知与取消、异常隔离、跨布局槽位拒绝。
 */
class MenuSyncTest {

    @Test
    void attachInitializesStoreWithDefaultsAndFirstRefreshIsNoop() {
        SyncLayout.Builder builder = SyncLayout.builder();
        SyncSlot<Integer> progress = builder.slot("progress", SyncCodecs.intRange(0, 100), 0);
        SyncSlot<Float> speed = builder.slot("speed", SyncCodecs.floatRange(0f, 5f), 1f);
        SyncSlot<Boolean> active = builder.slot("active", SyncCodecs.bools(), false);
        SyncLayout layout = builder.build();
        TestIntStore store = new TestIntStore(3);

        MenuSync sync = MenuSync.attach(layout, store);

        assertEquals(3, sync.layout().slotCount());
        assertEquals(0, sync.get(progress));
        assertEquals(1f, sync.get(speed), 0f);
        assertFalse(sync.get(active));
        assertArrayEquals(new int[]{0, Float.floatToRawIntBits(1f), 0}, store.snapshot());
        assertEquals(0, sync.refresh(), "attach 后首次 refresh 不应有变化");
    }

    @Test
    void setEncodesValueThroughCodecAndGetDecodesIt() {
        SyncLayout.Builder builder = SyncLayout.builder();
        SyncSlot<Integer> progress = builder.slot("progress", SyncCodecs.intRange(0, 100), 0);
        SyncLayout layout = builder.build();
        TestIntStore store = new TestIntStore(1);
        MenuSync sync = MenuSync.attach(layout, store);

        sync.set(progress, 42);
        assertEquals(42, store.get(progress.index()));
        assertEquals(42, sync.get(progress));

        sync.set(progress, 500);
        assertEquals(100, store.get(progress.index()), "写侧必须按编解码器钳制");
    }

    @Test
    void refreshDetectsExternalWritesAndNotifiesListenersWithOldAndNew() {
        SyncLayout.Builder builder = SyncLayout.builder();
        SyncSlot<Integer> progress = builder.slot("progress", SyncCodecs.intRange(0, 100), 10);
        SyncSlot<Boolean> active = builder.slot("active", SyncCodecs.bools(), false);
        SyncLayout layout = builder.build();
        TestIntStore store = new TestIntStore(2);
        MenuSync sync = MenuSync.attach(layout, store);

        List<String> events = new ArrayList<>();
        sync.onChange(progress, (oldValue, newValue) -> events.add("progress " + oldValue + "->" + newValue));
        sync.onChange(active, (oldValue, newValue) -> events.add("active " + oldValue + "->" + newValue));

        store.set(progress.index(), 42); // 模拟客户端收到数据包后的数据槽写入

        assertEquals(1, sync.refresh());
        assertEquals(List.of("progress 10->42"), events);
        assertEquals(42, sync.get(progress));
    }

    @Test
    void refreshWithoutChangesIsNoop() {
        SyncLayout.Builder builder = SyncLayout.builder();
        SyncSlot<Integer> progress = builder.slot("progress", SyncCodecs.intRange(0, 100), 0);
        SyncLayout layout = builder.build();
        MenuSync sync = MenuSync.attach(layout, new TestIntStore(1));

        int[] calls = {0};
        sync.onChange(progress, (oldValue, newValue) -> calls[0]++);

        assertEquals(0, sync.refresh());
        assertEquals(0, sync.refresh());
        assertEquals(0, calls[0]);
    }

    @Test
    void multipleListenersOnSameSlotAllReceiveEvent() {
        SyncLayout.Builder builder = SyncLayout.builder();
        SyncSlot<Integer> progress = builder.slot("progress", SyncCodecs.intRange(0, 100), 0);
        SyncLayout layout = builder.build();
        TestIntStore store = new TestIntStore(1);
        MenuSync sync = MenuSync.attach(layout, store);

        List<String> events = new ArrayList<>();
        sync.onChange(progress, (oldValue, newValue) -> events.add("a:" + newValue));
        sync.onChange(progress, (oldValue, newValue) -> events.add("b:" + newValue));

        store.set(progress.index(), 7);
        assertEquals(1, sync.refresh());
        assertEquals(List.of("a:7", "b:7"), events);
    }

    @Test
    void cancelledSubscriptionStopsReceivingEvents() {
        SyncLayout.Builder builder = SyncLayout.builder();
        SyncSlot<Integer> progress = builder.slot("progress", SyncCodecs.intRange(0, 100), 0);
        SyncLayout layout = builder.build();
        TestIntStore store = new TestIntStore(1);
        MenuSync sync = MenuSync.attach(layout, store);

        int[] calls = {0};
        SyncSubscription subscription = sync.onChange(progress, (oldValue, newValue) -> calls[0]++);
        assertTrue(subscription.active());

        store.set(progress.index(), 1);
        sync.refresh();
        assertEquals(1, calls[0]);

        subscription.cancel();
        subscription.cancel(); // 幂等
        assertFalse(subscription.active());

        store.set(progress.index(), 2);
        sync.refresh();
        assertEquals(1, calls[0], "取消后不应再收到回调");
    }

    @Test
    void listenerExceptionDoesNotBlockOtherListenersOrSyncFlow() {
        SyncLayout.Builder builder = SyncLayout.builder();
        SyncSlot<Integer> progress = builder.slot("progress", SyncCodecs.intRange(0, 100), 0);
        SyncLayout layout = builder.build();
        TestIntStore store = new TestIntStore(1);
        MenuSync sync = MenuSync.attach(layout, store);

        List<String> events = new ArrayList<>();
        sync.onChange(progress, (oldValue, newValue) -> {
            throw new IllegalStateException("boom");
        });
        sync.onChange(progress, (oldValue, newValue) -> events.add("ok:" + newValue));

        store.set(progress.index(), 9);
        assertEquals(1, sync.refresh());
        assertEquals(List.of("ok:9"), events);
    }

    @Test
    void rejectsSlotsFromOtherLayouts() {
        SyncLayout.Builder builder = SyncLayout.builder();
        builder.slot("progress", SyncCodecs.intRange(0, 100), 0);
        SyncLayout layout = builder.build();
        MenuSync sync = MenuSync.attach(layout, new TestIntStore(1));

        SyncLayout.Builder otherBuilder = SyncLayout.builder();
        SyncSlot<Integer> foreign = otherBuilder.slot("other", SyncCodecs.ints(), 5);
        otherBuilder.build();

        assertThrows(IllegalArgumentException.class, () -> sync.get(foreign));
        assertThrows(IllegalArgumentException.class, () -> sync.set(foreign, 1));
        assertThrows(IllegalArgumentException.class, () -> sync.onChange(foreign, (oldValue, newValue) -> {
        }));
    }

    @Test
    void rejectsUndersizedStore() {
        SyncLayout.Builder builder = SyncLayout.builder();
        builder.slot("progress", SyncCodecs.ints(), 0);
        builder.slot("speed", SyncCodecs.floats(), 0f);
        SyncLayout layout = builder.build();

        assertThrows(IllegalArgumentException.class, () -> MenuSync.attach(layout, new TestIntStore(1)));
    }

    @Test
    void rejectsNullArguments() {
        SyncLayout.Builder builder = SyncLayout.builder();
        SyncSlot<Integer> progress = builder.slot("progress", SyncCodecs.ints(), 0);
        SyncLayout layout = builder.build();
        MenuSync sync = MenuSync.attach(layout, new TestIntStore(1));

        assertThrows(NullPointerException.class, () -> MenuSync.attach(null, new TestIntStore(1)));
        assertThrows(NullPointerException.class, () -> MenuSync.attach(layout, null));
        assertThrows(NullPointerException.class, () -> sync.get(null));
        assertThrows(NullPointerException.class, () -> sync.set(progress, null));
        assertThrows(NullPointerException.class, () -> sync.onChange(progress, null));
    }
}
