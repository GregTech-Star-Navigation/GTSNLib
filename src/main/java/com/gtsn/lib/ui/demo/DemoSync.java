package com.gtsn.lib.ui.demo;

import com.gtsn.lib.ui.sync.SyncCodecs;
import com.gtsn.lib.ui.sync.SyncLayout;
import com.gtsn.lib.ui.sync.SyncSlot;

/**
 * 数据同步演示的槽位布局与服务端推进规则。
 *
 * <p>布局覆盖核心类型：区间 int（进度）、区间 float（速度）、bool（运行）、枚举（阶段）
 * 与索引（档位）。推进规则为纯函数，可在无 MC 环境单测；{@link DemoMenu} 在服务端每
 * {@link #PROGRESS_PERIOD_TICKS} tick 调用一次。</p>
 */
public final class DemoSync {

    /** 进度最小值。 */
    public static final int PROGRESS_MIN = 0;
    /** 进度最大值。 */
    public static final int PROGRESS_MAX = 100;
    /** 每次推进的步长。 */
    public static final int PROGRESS_STEP = 5;
    /** 服务端推进周期（tick）。 */
    public static final int PROGRESS_PERIOD_TICKS = 10;
    /** 档位数量（index 编解码器的取值范围）。 */
    public static final int TIER_COUNT = 5;

    private static final SyncLayout.Builder BUILDER = SyncLayout.builder();

    /** 进度（0..100）。 */
    public static final SyncSlot<Integer> PROGRESS =
            BUILDER.slot("progress", SyncCodecs.intRange(PROGRESS_MIN, PROGRESS_MAX), PROGRESS_MIN);
    /** 速度（0..5）。 */
    public static final SyncSlot<Float> SPEED =
            BUILDER.slot("speed", SyncCodecs.floatRange(0f, 5f), 1f);
    /** 运行开关。 */
    public static final SyncSlot<Boolean> ACTIVE =
            BUILDER.slot("active", SyncCodecs.bools(), true);
    /** 阶段（枚举编解码）。 */
    public static final SyncSlot<DemoPhase> PHASE =
            BUILDER.slot("phase", SyncCodecs.enums(DemoPhase.class), DemoPhase.IDLE);
    /** 档位索引（0..TIER_COUNT-1）。 */
    public static final SyncSlot<Integer> TIER =
            BUILDER.slot("tier", SyncCodecs.index(TIER_COUNT), 0);
    /** 演示布局（5 个数据槽）。 */
    public static final SyncLayout LAYOUT = BUILDER.build();

    private DemoSync() {
    }

    /** 推进进度：超过最大值回卷到最小值（100 → 0）。 */
    public static int advanceProgress(int current, int step) {
        if (step <= 0) {
            throw new IllegalArgumentException("step must be positive: " + step);
        }
        int next = current + step;
        return next > PROGRESS_MAX ? PROGRESS_MIN : next;
    }

    /** 进度对应的阶段：0 = IDLE，100 = DONE，其余 RUNNING。 */
    public static DemoPhase phaseFor(int progress) {
        if (progress <= PROGRESS_MIN) {
            return DemoPhase.IDLE;
        }
        if (progress >= PROGRESS_MAX) {
            return DemoPhase.DONE;
        }
        return DemoPhase.RUNNING;
    }

    /** 下一档位（0..TIER_COUNT-1 循环）。 */
    public static int nextTier(int current) {
        return (Math.floorMod(current, TIER_COUNT) + 1) % TIER_COUNT;
    }

    /** 档位对应的速度（1..5）。 */
    public static float speedForTier(int tier) {
        return 1f + Math.floorMod(tier, TIER_COUNT);
    }
}
