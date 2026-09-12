package com.gtsn.lib.ui.sync.bind;

import com.gtsn.lib.ui.sync.MenuSync;
import com.gtsn.lib.ui.sync.SyncSlot;
import com.gtsn.lib.ui.widget.TextWidget;

import java.util.Objects;
import java.util.function.Function;

/**
 * 文本绑定：类型化同步值经格式化器写入 {@link TextWidget}。
 *
 * <p>经 {@link SyncBindings#label} 创建（工厂完成后应用初值）。</p>
 */
public final class LabelBinding<T> extends AbstractSyncBinding<T> {

    private final TextWidget text;
    private final Function<T, String> formatter;

    LabelBinding(MenuSync sync, SyncSlot<T> slot, TextWidget text, Function<T, String> formatter) {
        super(sync, slot);
        this.text = Objects.requireNonNull(text, "text");
        this.formatter = Objects.requireNonNull(formatter, "formatter");
    }

    /** 目标文本控件。 */
    public TextWidget target() {
        return text;
    }

    @Override
    protected void apply(T value) {
        text.text(formatter.apply(value));
    }
}
