package com.gtsn.lib.ui.sync.bind;

import com.gtsn.lib.ui.sync.MenuSync;
import com.gtsn.lib.ui.sync.SyncSlot;
import com.gtsn.lib.ui.widget.ProgressBarWidget;

import java.util.Objects;

/**
 * 进度条绑定：数值同步值（int/float 等）→ {@link ProgressBarWidget#value(double)}。
 *
 * <p>进度条自身的区间（{@code range(min, max)}）由使用方声明；绑定只写入数值。
 * 经 {@link SyncBindings#progressBar} 创建（工厂完成后应用初值）。</p>
 *
 * @param <N> 数值类型
 */
public final class ProgressBarBinding<N extends Number> extends AbstractSyncBinding<N> {

    private final ProgressBarWidget bar;

    ProgressBarBinding(MenuSync sync, SyncSlot<N> slot, ProgressBarWidget bar) {
        super(sync, slot);
        this.bar = Objects.requireNonNull(bar, "bar");
    }

    /** 目标进度条控件。 */
    public ProgressBarWidget target() {
        return bar;
    }

    @Override
    protected void apply(N value) {
        bar.value(value.doubleValue());
    }
}
