package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.CrossAxisAlign;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.MainAxisAlign;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.SlotIcon;
import com.gtsn.lib.ui.theme.ThemeColorRole;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 机器状态面板：把一个 MC-free 的 {@link MachineStatusView} 装配为 GTSN UI 控件
 * （标题 / 状态 / 能量条 / 配方进度箭头 / 流体罐 / 物品槽网格）。
 *
 * <p>只读展示：面板不反向操作机器，也不含交互控件。本类**不引用任何 {@code net.minecraft} 类型**
 * （#22 评审 F1-3 恢复 {@code ui.widget} 纯度）：物品 / 流体的 MC 类型翻译由调用方在 binder 层
 * 完成（客户端用 {@code MachineStatusViewBinder}），组件因此在无游戏环境可测。</p>
 */
public final class MachineStatusPanel extends AbstractWidget {

    private static final int SLOT_COLUMNS = 9;

    private final MachineStatusView view;
    private final TextWidget titleText;
    private final TextWidget statusText;
    private final EnergyBarWidget energyBar;
    private final ProgressArrowWidget progressArrow;
    private final TextWidget progressText;
    private final List<TankWidget> tanks;
    private final MachineSlotsPanel slotsPanel;

    public MachineStatusPanel(MachineStatusView view, TextMetrics metrics) {
        this.view = Objects.requireNonNull(view, "view");
        Objects.requireNonNull(metrics, "metrics");

        PanelWidget panel = new PanelWidget()
                .background(ThemeColorRole.PANEL_BACKGROUND)
                .border(ThemeColorRole.PANEL_BORDER, 1)
                .padding(Insets.all(6))
                .gap(4);

        this.titleText = new TextWidget(view.machineId(), metrics).colorRole(ThemeColorRole.TEXT_STRONG);
        this.statusText = new TextWidget(view.status(), metrics).colorRole(ThemeColorRole.TEXT_MUTED);
        this.energyBar = new EnergyBarWidget()
                .energy(view.energyStored(), view.energyCapacity())
                .size(Sizing.fill(), Sizing.fixed(12));
        this.progressArrow = new ProgressArrowWidget()
                .progress(view.progress(), view.maxProgress())
                .working(view.working())
                .fixedSize(22, 16);
        this.progressText = new TextWidget(progressLabel(), metrics).colorRole(ThemeColorRole.TEXT);

        Stack progressRow = Stack.horizontal()
                .gap(6)
                .align(MainAxisAlign.START, CrossAxisAlign.CENTER);
        progressRow.add(progressArrow);
        progressRow.add(progressText);

        Stack tankRow = Stack.horizontal()
                .gap(6)
                .align(MainAxisAlign.START, CrossAxisAlign.CENTER);
        List<TankWidget> builtTanks = new ArrayList<>();
        for (MachineStatusView.Tank fluid : view.tanks()) {
            TankWidget tank = new TankWidget()
                    .tank(fluid.amount(), fluid.capacity())
                    .fluidName(fluid.name())
                    .fixedSize(18, 40);
            builtTanks.add(tank);
            Stack column = Stack.vertical().gap(2).align(MainAxisAlign.CENTER, CrossAxisAlign.CENTER);
            column.add(tank);
            column.add(new TextWidget(tank.fluidName(), metrics).colorRole(ThemeColorRole.TEXT_MUTED));
            tankRow.add(column);
        }
        this.tanks = Collections.unmodifiableList(builtTanks);

        List<SlotIcon> icons = new ArrayList<>();
        for (MachineStatusView.Slot slot : view.itemSlots()) {
            while (icons.size() <= slot.index()) {
                icons.add(null);
            }
            icons.set(slot.index(), slot.icon());
        }
        int columns = icons.isEmpty() ? 1 : Math.min(icons.size(), SLOT_COLUMNS);
        this.slotsPanel = new MachineSlotsPanel(icons, columns);

        panel.add(titleText);
        panel.add(statusText);
        panel.add(energyBar);
        panel.add(progressRow);
        if (!tanks.isEmpty()) {
            panel.add(tankRow);
        }
        panel.add(slotsPanel);
        add(panel);
    }

    public MachineStatusView view() {
        return view;
    }

    public TextWidget titleText() {
        return titleText;
    }

    public TextWidget statusText() {
        return statusText;
    }

    public EnergyBarWidget energyBar() {
        return energyBar;
    }

    public ProgressArrowWidget progressArrow() {
        return progressArrow;
    }

    public TextWidget progressText() {
        return progressText;
    }

    public List<TankWidget> tanks() {
        return tanks;
    }

    public MachineSlotsPanel slotsPanel() {
        return slotsPanel;
    }

    public MachineStatusPanel fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public MachineStatusPanel size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public MachineStatusPanel fill() {
        node().params().fill();
        return this;
    }

    private String progressLabel() {
        return view.progress() + " / " + view.maxProgress();
    }
}
