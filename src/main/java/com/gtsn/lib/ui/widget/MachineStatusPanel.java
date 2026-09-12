package com.gtsn.lib.ui.widget;

import com.gtsn.lib.api.GtMachineSnapshot;
import com.gtsn.lib.ui.layout.CrossAxisAlign;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.MainAxisAlign;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.SlotIcon;
import com.gtsn.lib.ui.theme.ThemeColorRole;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 机器状态面板：把一个 {@link GtMachineSnapshot} 装配为 GTSN UI 控件（标题 / 状态 / 能量条 /
 * 配方进度箭头 / 流体罐 / 物品槽网格）。
 *
 * <p>只读展示：面板不反向操作机器，也不含交互控件。物品槽图标经调用方注入的映射器
 * （客户端用 {@code ItemStackIcon::of}）把 {@code ItemStack} 翻译为 {@code SlotIcon}，故面板
 * 本身可无游戏环境测试。</p>
 */
public final class MachineStatusPanel extends AbstractWidget {

    private static final int SLOT_COLUMNS = 9;

    private final GtMachineSnapshot snapshot;
    private final TextWidget titleText;
    private final TextWidget statusText;
    private final EnergyBarWidget energyBar;
    private final ProgressArrowWidget progressArrow;
    private final TextWidget progressText;
    private final List<TankWidget> tanks;
    private final MachineSlotsPanel slotsPanel;

    public MachineStatusPanel(GtMachineSnapshot snapshot, TextMetrics metrics,
                              Function<ItemStack, SlotIcon> iconMapper) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(metrics, "metrics");

        PanelWidget panel = new PanelWidget()
                .background(ThemeColorRole.PANEL_BACKGROUND)
                .border(ThemeColorRole.PANEL_BORDER, 1)
                .padding(Insets.all(6))
                .gap(4);

        this.titleText = new TextWidget(snapshot.machineId(), metrics).colorRole(ThemeColorRole.TEXT_STRONG);
        this.statusText = new TextWidget(snapshot.status(), metrics).colorRole(ThemeColorRole.TEXT_MUTED);
        this.energyBar = new EnergyBarWidget()
                .energy(snapshot.energyStored(), snapshot.energyCapacity())
                .size(Sizing.fill(), Sizing.fixed(12));
        this.progressArrow = new ProgressArrowWidget()
                .progress(snapshot.progress(), snapshot.maxProgress())
                .working(snapshot.working())
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
        for (int index = 0; index < snapshot.fluidTanks().size(); index++) {
            FluidStack fluid = snapshot.fluidTanks().get(index);
            TankWidget tank = new TankWidget()
                    .tank(fluid.getAmount(), snapshot.fluidCapacity(index))
                    .fluidName(fluidName(fluid))
                    .fixedSize(18, 40);
            builtTanks.add(tank);
            Stack column = Stack.vertical().gap(2).align(MainAxisAlign.CENTER, CrossAxisAlign.CENTER);
            column.add(tank);
            column.add(new TextWidget(tank.fluidName(), metrics).colorRole(ThemeColorRole.TEXT_MUTED));
            tankRow.add(column);
        }
        this.tanks = Collections.unmodifiableList(builtTanks);

        List<SlotIcon> icons = new ArrayList<>();
        for (ItemStack stack : snapshot.itemSlots()) {
            icons.add(iconMapper == null || stack.isEmpty() ? null : iconMapper.apply(stack));
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

    public GtMachineSnapshot snapshot() {
        return snapshot;
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
        return snapshot.progress() + " / " + snapshot.maxProgress();
    }

    private static String fluidName(FluidStack fluid) {
        if (fluid == null || fluid.isEmpty()) {
            return "";
        }
        return fluid.getDisplayName().getString();
    }
}
