package com.gtsn.lib.ui.demo;

import com.gtsn.lib.ui.layout.Anchor;
import com.gtsn.lib.ui.layout.CrossAxisAlign;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.MainAxisAlign;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.widget.BoxWidget;
import com.gtsn.lib.ui.widget.ButtonWidget;
import com.gtsn.lib.ui.widget.CheckboxWidget;
import com.gtsn.lib.ui.widget.ClipWidget;
import com.gtsn.lib.ui.widget.DividerWidget;
import com.gtsn.lib.ui.widget.ItemSlotWidget;
import com.gtsn.lib.ui.widget.PanelWidget;
import com.gtsn.lib.ui.widget.ProgressBarWidget;
import com.gtsn.lib.ui.widget.ScrollPanelWidget;
import com.gtsn.lib.ui.widget.Stack;
import com.gtsn.lib.ui.widget.TextAlign;
import com.gtsn.lib.ui.widget.TextMetrics;
import com.gtsn.lib.ui.widget.TextWidget;
import com.gtsn.lib.ui.widget.ToggleSwitchWidget;
import com.gtsn.lib.ui.widget.Tooltip;
import com.gtsn.lib.ui.widget.Widget;

import java.util.Objects;

/**
 * 组件库开发测试界面的内容装配：面板 / 文本（换行/对齐）/ 按钮（四态）/ 进度条 /
 * 物品槽（选择/工具提示）/ 滚动容器 / 复选框 / 开关 / 分隔线 / 占位 / 裁剪与锚定演示。
 *
 * <p>纯控件树，不依赖 Minecraft；客户端窗口与 GameTest 共用同一份装配保证行为一致。
 * 物品图标经 {@link DemoIcons} 注入，客户端可传入真实物品堆叠渲染。</p>
 */
public final class DemoContent {

    private final Widget root;
    private final DemoState state;

    private final ButtonWidget clickButton;
    private final ButtonWidget toggleButton;
    private final ButtonWidget clearButton;
    private final ButtonWidget disabledButton;
    private final ButtonWidget progressUpButton;
    private final ButtonWidget progressResetButton;

    private final CheckboxWidget checkbox;
    private final CheckboxWidget logCheckbox;
    private final ToggleSwitchWidget toggleSwitch;
    private final ToggleSwitchWidget disabledToggle;

    private final ProgressBarWidget progressBar;
    private final KeypadWidget keypad;

    private final TextWidget clickStatus;
    private final TextWidget toggleStatus;
    private final TextWidget selectionStatus;

    private final ItemSlotWidget itemSlot1;
    private final ItemSlotWidget itemSlot2;
    private final ItemSlotWidget disabledItemSlot;

    private final ScrollPanelWidget scrollPanel;
    private final ClipWidget clipShowcase;
    private final Widget badge;

    private DemoContent(Builder builder) {
        this.root = builder.root;
        this.state = builder.state;
        this.clickButton = builder.clickButton;
        this.toggleButton = builder.toggleButton;
        this.clearButton = builder.clearButton;
        this.disabledButton = builder.disabledButton;
        this.progressUpButton = builder.progressUpButton;
        this.progressResetButton = builder.progressResetButton;
        this.checkbox = builder.checkbox;
        this.logCheckbox = builder.logCheckbox;
        this.toggleSwitch = builder.toggleSwitch;
        this.disabledToggle = builder.disabledToggle;
        this.progressBar = builder.progressBar;
        this.keypad = builder.keypad;
        this.clickStatus = builder.clickStatus;
        this.toggleStatus = builder.toggleStatus;
        this.selectionStatus = builder.selectionStatus;
        this.itemSlot1 = builder.itemSlot1;
        this.itemSlot2 = builder.itemSlot2;
        this.disabledItemSlot = builder.disabledItemSlot;
        this.scrollPanel = builder.scrollPanel;
        this.clipShowcase = builder.clipShowcase;
        this.badge = builder.badge;
    }

    public static DemoContent build(TextMetrics metrics) {
        return build(new DemoState(), metrics);
    }

    public static DemoContent build(DemoState state, TextMetrics metrics) {
        return build(state, metrics, DemoIcons.placeholders());
    }

    public static DemoContent build(DemoState state, TextMetrics metrics, DemoIcons icons) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(metrics, "metrics");
        Objects.requireNonNull(icons, "icons");
        return new Builder(state, metrics, icons).assemble();
    }

    public Widget root() {
        return root;
    }

    public DemoState state() {
        return state;
    }

    public ButtonWidget clickButton() {
        return clickButton;
    }

    public ButtonWidget toggleButton() {
        return toggleButton;
    }

    public ButtonWidget clearButton() {
        return clearButton;
    }

    public ButtonWidget disabledButton() {
        return disabledButton;
    }

    public ButtonWidget progressUpButton() {
        return progressUpButton;
    }

    public ButtonWidget progressResetButton() {
        return progressResetButton;
    }

    public CheckboxWidget checkbox() {
        return checkbox;
    }

    public CheckboxWidget logCheckbox() {
        return logCheckbox;
    }

    public ToggleSwitchWidget toggleSwitch() {
        return toggleSwitch;
    }

    public ToggleSwitchWidget disabledToggle() {
        return disabledToggle;
    }

    public ProgressBarWidget progressBar() {
        return progressBar;
    }

    public KeypadWidget keypad() {
        return keypad;
    }

    public TextWidget clickStatus() {
        return clickStatus;
    }

    public TextWidget toggleStatus() {
        return toggleStatus;
    }

    public TextWidget selectionStatus() {
        return selectionStatus;
    }

    public ItemSlotWidget itemSlot1() {
        return itemSlot1;
    }

    public ItemSlotWidget itemSlot2() {
        return itemSlot2;
    }

    public ItemSlotWidget disabledItemSlot() {
        return disabledItemSlot;
    }

    public ScrollPanelWidget scrollPanel() {
        return scrollPanel;
    }

    public ClipWidget clipShowcase() {
        return clipShowcase;
    }

    public Widget badge() {
        return badge;
    }

    /** 组装过程内部使用，避免超长方法中的前向引用问题。 */
    private static final class Builder {

        private static final int PANEL_BG = 0xFF181C22;
        private static final int PANEL_BORDER = 0xFF2E3846;
        private static final int TEXT_MUTED = 0xFF9FB0C0;

        private final DemoState state;
        private final TextMetrics metrics;
        private final DemoIcons icons;

        private Stack root;
        private ButtonWidget clickButton;
        private ButtonWidget toggleButton;
        private ButtonWidget clearButton;
        private ButtonWidget disabledButton;
        private ButtonWidget progressUpButton;
        private ButtonWidget progressResetButton;
        private CheckboxWidget checkbox;
        private CheckboxWidget logCheckbox;
        private ToggleSwitchWidget toggleSwitch;
        private ToggleSwitchWidget disabledToggle;
        private ProgressBarWidget progressBar;
        private KeypadWidget keypad;
        private TextWidget clickStatus;
        private TextWidget toggleStatus;
        private TextWidget selectionStatus;
        private ItemSlotWidget itemSlot1;
        private ItemSlotWidget itemSlot2;
        private ItemSlotWidget disabledItemSlot;
        private ScrollPanelWidget scrollPanel;
        private ClipWidget clipShowcase;
        private Widget badge;

        private Builder(DemoState state, TextMetrics metrics, DemoIcons icons) {
            this.state = state;
            this.metrics = metrics;
            this.icons = icons;
        }

        private DemoContent assemble() {
            root = Stack.horizontal().padding(Insets.all(8)).gap(8);
            buildLeftColumn();
            buildRightColumn();
            buildBadge();
            return new DemoContent(this);
        }

        private void buildLeftColumn() {
            Stack left = root.add(Stack.vertical().gap(6)
                    .size(Sizing.fixed(300), Sizing.wrap())
                    .crossAxisAlign(CrossAxisAlign.STRETCH));
            left.add(new TextWidget("GTSN UI 组件库", metrics).shadow(true).color(0xFFE8F0F8));

            // 先在局部变量中构造（供按钮回调引用），再按布局顺序加入面板。
            clickStatus = new TextWidget("点击次数: 0", metrics).color(TEXT_MUTED);
            toggleStatus = new TextWidget("状态: A", metrics).color(TEXT_MUTED);
            progressBar = new ProgressBarWidget().range(0, 1).value(state.progress())
                    .label(progress -> Math.round(progress * 100) + "%")
                    .size(Sizing.fill(), Sizing.fixed(12));

            PanelWidget buttonPanel = left.add(panel("按钮 (Button)"));
            Stack buttonRow1 = buttonPanel.add(Stack.horizontal().gap(6));
            clickButton = buttonRow1.add(new ButtonWidget("点击 +1", metrics, () -> {
                state.incrementClicks();
                clickStatus.text("点击次数: " + state.clicks());
            }));
            toggleButton = buttonRow1.add(new ButtonWidget("切换状态", metrics, () -> {
                state.toggle();
                toggleStatus.text("状态: " + (state.toggled() ? "B" : "A"))
                        .color(state.toggled() ? 0xFF7FD08A : TEXT_MUTED);
            }));
            clearButton = buttonRow1.add(new ButtonWidget("清空", metrics, () -> {
                state.clearTyped();
                state.lastKey("(none)");
            }));

            Stack buttonRow2 = buttonPanel.add(Stack.horizontal().gap(6));
            disabledButton = buttonRow2.add(new ButtonWidget("禁用按钮", metrics, () -> {
            }).enabled(false).tooltip(Tooltip.of("禁用态：不响应输入", "状态报告为 DISABLED")));
            progressUpButton = buttonRow2.add(new ButtonWidget("进度+10%", metrics, () -> {
                state.advanceProgress(0.1);
                progressBar.value(state.progress());
            }).tooltip(Tooltip.of("进度条：点击累加", "上限自动钳制")));
            progressResetButton = buttonRow2.add(new ButtonWidget("重置", metrics, () -> {
                state.progress(0);
                progressBar.value(0);
            }));
            buttonPanel.add(new DividerWidget().color(PANEL_BORDER));
            buttonPanel.add(clickStatus);
            buttonPanel.add(toggleStatus);
            buttonPanel.add(progressBar);

            PanelWidget selectionPanel = left.add(panel("选择 (Checkbox / Toggle)"));
            checkbox = selectionPanel.add(new CheckboxWidget("启用模块", metrics, value -> {
            }).tooltip(Tooltip.of("复选框：点击或空格切换")));
            logCheckbox = selectionPanel.add(new CheckboxWidget("记录日志", metrics, value -> {
            }).checked(true).tooltip(Tooltip.of("默认勾选的复选框")));
            Stack switchRow = selectionPanel.add(Stack.horizontal().gap(10));
            toggleSwitch = switchRow.add(new ToggleSwitchWidget("自动运行", metrics, value -> {
            }).tooltip(Tooltip.of("开关：轨道 + 滑块")));
            disabledToggle = switchRow.add(new ToggleSwitchWidget("锁定", metrics, value -> {
            }).checked(true).enabled(false));

            PanelWidget textPanel = left.add(panel("文本 (Text)"));
            textPanel.add(new TextWidget(
                    "这是一段用于演示自动换行与行距的长文本：宽度受限时按词断行，超长单词按字符硬断。", metrics)
                    .wrap(260).color(0xFFC8D4E0));
            Stack alignRow = textPanel.add(Stack.horizontal().gap(8));
            alignRow.add(new TextWidget("左对齐", metrics).size(Sizing.fixed(72), Sizing.wrap()));
            alignRow.add(new TextWidget("居中", metrics).align(TextAlign.CENTER)
                    .size(Sizing.fixed(72), Sizing.wrap()).color(0xFFB8C8D8));
            alignRow.add(new TextWidget("右对齐", metrics).align(TextAlign.RIGHT)
                    .size(Sizing.fixed(72), Sizing.wrap()).color(0xFF8FB0C8));
            textPanel.add(new DividerWidget().color(PANEL_BORDER));
            textPanel.add(new TextWidget("分隔线下方：Divider 基元", metrics).color(TEXT_MUTED));

            PanelWidget inputPanel = left.add(panel("输入 (Input)"));
            keypad = inputPanel.add(new KeypadWidget(state));
        }

        private void buildRightColumn() {
            Stack right = root.add(Stack.vertical().gap(6).weight(1)
                    .crossAxisAlign(CrossAxisAlign.STRETCH));

            selectionStatus = new TextWidget("已选槽位: 无", metrics).color(TEXT_MUTED);
            PanelWidget itemPanel = right.add(panel("物品槽 (ItemSlot)"));
            Stack slotRow = itemPanel.add(Stack.horizontal().gap(6));
            itemSlot1 = slotRow.add(new ItemSlotWidget().icon(icons.primary()).selectable(true)
                    .tooltip(Tooltip.of("钻石 ×3", "点击选择此槽位")));
            itemSlot2 = slotRow.add(new ItemSlotWidget().icon(icons.secondary()).selectable(true)
                    .tooltip(Tooltip.of("红石 ×16", "选择互斥演示")));
            disabledItemSlot = slotRow.add(new ItemSlotWidget().icon(icons.primary()).enabled(false)
                    .tooltip(Tooltip.of("禁用槽位")));
            itemPanel.add(selectionStatus);
            itemSlot1.onSelectionChanged(selected -> {
                if (selected) {
                    itemSlot2.selected(false);
                }
                updateSelectionStatus();
            });
            itemSlot2.onSelectionChanged(selected -> {
                if (selected) {
                    itemSlot1.selected(false);
                }
                updateSelectionStatus();
            });

            PanelWidget scrollPanelHolder = right.add(panel("滚动 (ScrollPanel)"));
            scrollPanel = scrollPanelHolder.add(new ScrollPanelWidget().size(Sizing.fill(), Sizing.fixed(90)));
            Stack scrollContent = scrollPanel.add(Stack.vertical().gap(3));
            for (int i = 1; i <= 12; i++) {
                Stack row = scrollContent.add(Stack.horizontal().gap(6).crossAxisAlign(CrossAxisAlign.CENTER));
                row.add(new BoxWidget().fixedSize(6, 10).fill(i % 2 == 0 ? 0xFF4C74A8 : 0xFF6F8F5F));
                row.add(new TextWidget("滚动条目 " + i, metrics).color(0xFFC8D4E0));
            }

            PanelWidget layoutPanel = right.add(panel("布局 (Layout / Clip)"));
            clipShowcase = layoutPanel.add(new ClipWidget().size(Sizing.fill(), Sizing.fixed(70))
                    .padding(Insets.all(4)));
            Stack showcaseColumn = clipShowcase.add(Stack.vertical().gap(4));
            Stack boxesRow = showcaseColumn.add(Stack.horizontal().gap(6));
            boxesRow.add(new BoxWidget().fixedSize(46, 20).fill(0xFF4878A8).border(0xFF9CC4E4, 1));
            boxesRow.add(new BoxWidget().size(Sizing.fill(), Sizing.fixed(20)).weight(1)
                    .fill(0xFF507850).border(0xFF9CC4E4, 1));
            boxesRow.add(new BoxWidget().fixedSize(46, 20).fill(0xFFB08A48).border(0xFF9CC4E4, 1));
            showcaseColumn.add(new BoxWidget().fixedSize(500, 30).fill(0xFF803030));
        }

        private void buildBadge() {
            Stack badgeStack = root.add(Stack.vertical().fixedSize(150, 16)
                    .align(MainAxisAlign.CENTER, CrossAxisAlign.CENTER)
                    .absolute(Anchor.BOTTOM_RIGHT, -4, -4));
            badgeStack.add(new TextWidget("components #17", metrics).color(0xFF8FB0C8));
            badge = badgeStack;
        }

        private void updateSelectionStatus() {
            String label = "无";
            if (itemSlot1.isSelected()) {
                label = "1";
            } else if (itemSlot2.isSelected()) {
                label = "2";
            }
            selectionStatus.text("已选槽位: " + label);
        }

        private PanelWidget panel(String title) {
            return new PanelWidget().title(title, metrics).padding(Insets.all(6)).gap(4)
                    .background(PANEL_BG).border(PANEL_BORDER, 1);
        }
    }
}
