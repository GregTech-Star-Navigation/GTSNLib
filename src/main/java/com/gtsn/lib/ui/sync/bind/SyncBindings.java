package com.gtsn.lib.ui.sync.bind;

import com.gtsn.lib.ui.sync.MenuSync;
import com.gtsn.lib.ui.sync.SyncSlot;
import com.gtsn.lib.ui.widget.ProgressBarWidget;
import com.gtsn.lib.ui.widget.TextWidget;

import java.util.Objects;
import java.util.function.Function;

/**
 * 绑定工厂：把同步值与 GTSN UI 控件连接起来，构造完成后立即应用当前值。
 *
 * <p>控件均为纯 Java（不依赖 Minecraft），绑定层可无 MC 单测；
 * 屏幕在 tick 中经 {@link SyncBindingGroup#refresh()} 驱动更新。</p>
 */
public final class SyncBindings {

    private SyncBindings() {
    }

    /** 进度条绑定：数值同步值（int/float 等）→ {@link ProgressBarWidget#value(double)}。 */
    public static <N extends Number> SyncBinding progressBar(MenuSync sync, SyncSlot<N> slot, ProgressBarWidget bar) {
        Objects.requireNonNull(bar, "bar");
        ProgressBarBinding<N> binding = new ProgressBarBinding<>(sync, slot, bar);
        binding.refresh();
        return binding;
    }

    /** 文本绑定：类型化同步值 → 格式化字符串 → {@link TextWidget#text(String)}。 */
    public static <T> SyncBinding label(MenuSync sync, SyncSlot<T> slot, TextWidget text,
                                        Function<T, String> formatter) {
        Objects.requireNonNull(formatter, "formatter");
        LabelBinding<T> binding = new LabelBinding<>(sync, slot, text, formatter);
        binding.refresh();
        return binding;
    }
}
