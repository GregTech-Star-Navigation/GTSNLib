package com.gtsn.lib.ui.demo;

import com.gtsn.lib.ui.layout.CrossAxisAlign;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.MainAxisAlign;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.sync.MenuSync;
import com.gtsn.lib.ui.sync.bind.SyncBindingGroup;
import com.gtsn.lib.ui.sync.bind.SyncBindings;
import com.gtsn.lib.ui.theme.Spacing;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeColorRole;
import com.gtsn.lib.ui.theme.ThemeContext;
import com.gtsn.lib.ui.widget.PanelWidget;
import com.gtsn.lib.ui.widget.ProgressBarWidget;
import com.gtsn.lib.ui.widget.Stack;
import com.gtsn.lib.ui.widget.TextMetrics;
import com.gtsn.lib.ui.widget.TextWidget;
import com.gtsn.lib.ui.widget.Widget;

import java.util.Objects;
import java.util.function.Function;

/**
 * 数据同步演示界面的内容装配（纯控件树，不依赖 Minecraft）。
 *
 * <p>{@link #bind(MenuSync)} 把 {@link DemoSync} 的各类型槽位接到进度条 / 文本控件上
 * （区间 int → 进度条与文本、枚举 → 阶段文本、index → 档位文本、float → 速度文本、
 * bool → 运行文本），返回绑定组供屏幕 tick 驱动。客户端屏幕与单测共用同一份装配。</p>
 */
public final class SyncDemoContent {

    private static final int COLUMN_WIDTH = 320;

    private final Widget root;
    private final ProgressBarWidget progressBar;
    private final TextWidget progressLabel;
    private final TextWidget phaseLabel;
    private final TextWidget tierLabel;
    private final TextWidget speedLabel;
    private final TextWidget activeLabel;

    private SyncDemoContent(Builder builder) {
        this.root = builder.root;
        this.progressBar = builder.progressBar;
        this.progressLabel = builder.progressLabel;
        this.phaseLabel = builder.phaseLabel;
        this.tierLabel = builder.tierLabel;
        this.speedLabel = builder.speedLabel;
        this.activeLabel = builder.activeLabel;
    }

    /** 用当前主题装配。 */
    public static SyncDemoContent build(TextMetrics metrics) {
        return build(metrics, ThemeContext.active());
    }

    /** 用指定主题装配。 */
    public static SyncDemoContent build(TextMetrics metrics, Theme theme) {
        Objects.requireNonNull(metrics, "metrics");
        Objects.requireNonNull(theme, "theme");
        return new Builder(metrics, theme).assemble();
    }

    /** 根控件。 */
    public Widget root() {
        return root;
    }

    /** 进度条（绑定进度槽位）。 */
    public ProgressBarWidget progressBar() {
        return progressBar;
    }

    /** 进度文本（绑定进度槽位）。 */
    public TextWidget progressLabel() {
        return progressLabel;
    }

    /** 阶段文本（绑定枚举槽位）。 */
    public TextWidget phaseLabel() {
        return phaseLabel;
    }

    /** 档位文本（绑定索引槽位）。 */
    public TextWidget tierLabel() {
        return tierLabel;
    }

    /** 速度文本（绑定 float 槽位）。 */
    public TextWidget speedLabel() {
        return speedLabel;
    }

    /** 运行状态文本（绑定 bool 槽位）。 */
    public TextWidget activeLabel() {
        return activeLabel;
    }

    /** 建立全部绑定（初始应用当前值）；屏幕在关闭时需 close 返回的绑定组。 */
    public SyncBindingGroup bind(MenuSync sync) {
        Objects.requireNonNull(sync, "sync");
        SyncBindingGroup group = SyncBindingGroup.of(sync);
        group.add(SyncBindings.progressBar(sync, DemoSync.PROGRESS, progressBar));
        group.add(SyncBindings.label(sync, DemoSync.PROGRESS, progressLabel,
                value -> "进度 " + value + " / " + DemoSync.PROGRESS_MAX));
        group.add(SyncBindings.label(sync, DemoSync.PHASE, phaseLabel, value -> "阶段 " + value));
        group.add(SyncBindings.label(sync, DemoSync.TIER, tierLabel,
                value -> "档位 " + (value + 1) + " / " + DemoSync.TIER_COUNT));
        group.add(SyncBindings.label(sync, DemoSync.SPEED, speedLabel, speedFormatter()));
        group.add(SyncBindings.label(sync, DemoSync.ACTIVE, activeLabel,
                value -> "运行 " + (value ? "是" : "否")));
        return group;
    }

    /** 速度格式化（独立出来便于单测 / 复用）。 */
    public static Function<Float, String> speedFormatter() {
        return value -> String.format("速度 %.1f", value);
    }

    private static final class Builder {

        private final TextMetrics metrics;
        private final Theme theme;

        private Widget root;
        private ProgressBarWidget progressBar;
        private TextWidget progressLabel;
        private TextWidget phaseLabel;
        private TextWidget tierLabel;
        private TextWidget speedLabel;
        private TextWidget activeLabel;

        private Builder(TextMetrics metrics, Theme theme) {
            this.metrics = metrics;
            this.theme = theme;
        }

        private int space(Spacing step) {
            return theme.spacing(step);
        }

        private SyncDemoContent assemble() {
            Stack screen = Stack.horizontal().padding(Insets.all(space(Spacing.LG)))
                    .align(MainAxisAlign.CENTER, CrossAxisAlign.CENTER);
            Stack column = screen.add(Stack.vertical().gap(space(Spacing.MD))
                    .size(Sizing.fixed(COLUMN_WIDTH), Sizing.wrap())
                    .crossAxisAlign(CrossAxisAlign.STRETCH));

            column.add(new TextWidget("GTSN UI 数据同步演示", metrics).shadow(true)
                    .colorRole(ThemeColorRole.TEXT_STRONG));
            column.add(new TextWidget("界面数值来自服务端容器菜单数据槽（每 0.5 秒推进）", metrics)
                    .colorRole(ThemeColorRole.TEXT_MUTED).wrap(COLUMN_WIDTH));

            PanelWidget panel = column.add(new PanelWidget()
                    .title("同步值 (AbstractContainerMenu)", metrics)
                    .padding(Insets.all(space(Spacing.MD)))
                    .gap(space(Spacing.SM))
                    .background(ThemeColorRole.PANEL_BACKGROUND)
                    .border(ThemeColorRole.PANEL_BORDER, 1));

            progressBar = panel.add(new ProgressBarWidget()
                    .range(DemoSync.PROGRESS_MIN, DemoSync.PROGRESS_MAX)
                    .gradient(true)
                    .label(progress -> Math.round(progress * 100) + "%")
                    .size(Sizing.fill(), Sizing.fixed(12)));
            progressLabel = panel.add(new TextWidget("", metrics));
            phaseLabel = panel.add(new TextWidget("", metrics));
            tierLabel = panel.add(new TextWidget("", metrics));
            speedLabel = panel.add(new TextWidget("", metrics));
            activeLabel = panel.add(new TextWidget("", metrics));

            column.add(new TextWidget("服务端权威值 → 数据槽 → 客户端控件", metrics)
                    .colorRole(ThemeColorRole.TEXT_MUTED));

            this.root = screen;
            return new SyncDemoContent(this);
        }
    }
}
