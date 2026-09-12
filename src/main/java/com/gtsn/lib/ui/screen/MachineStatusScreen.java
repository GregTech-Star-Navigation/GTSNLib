package com.gtsn.lib.ui.screen;

import com.gtsn.lib.api.GtMachineSnapshot;
import com.gtsn.lib.gt.adapter.GtMachineSnapshots;
import com.gtsn.lib.ui.layout.CrossAxisAlign;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.MainAxisAlign;
import com.gtsn.lib.ui.render.ItemStackIcon;
import com.gtsn.lib.ui.theme.ThemeColorRole;
import com.gtsn.lib.ui.widget.MachineStatusPanel;
import com.gtsn.lib.ui.widget.Stack;
import com.gtsn.lib.ui.widget.TextMetrics;
import com.gtsn.lib.ui.widget.TextWidget;
import com.gtsn.lib.ui.widget.Widget;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/**
 * 机器状态界面（GTSN UI，方案 A）：只读展示某台 GTCEu 机器的实时快照。
 *
 * <p>每 {@value #REFRESH_INTERVAL_TICKS} tick 从客户端镜像机器重新取快照并重建控件树——客户端读的是
 * LDLib 托管同步（{@code @DescSynced}）后的值，因此无需网络包、也不依赖 LDLib 的界面管线。</p>
 *
 * <p>只读边界：界面不含任何可操作控件（槽位 / 按钮均为展示），不打开 GT 的原生机器界面，也不修改机器状态。
 * 客户端专用类（类加载纪律）。</p>
 */
public final class MachineStatusScreen extends GtsnScreen {

    private static final int REFRESH_INTERVAL_TICKS = 5;

    private final BlockPos pos;
    private GtMachineSnapshot snapshot;
    private int refreshCountdown = REFRESH_INTERVAL_TICKS;

    public MachineStatusScreen(BlockPos pos) {
        this(pos, peek(pos));
    }

    private MachineStatusScreen(BlockPos pos, GtMachineSnapshot snapshot) {
        super(Component.literal("GTSN 机器状态"), buildRoot(snapshot, pos));
        this.pos = pos;
        this.snapshot = snapshot;
    }

    /** 当前展示的快照（打开时的首帧值）。 */
    public GtMachineSnapshot snapshot() {
        return snapshot;
    }

    public BlockPos machinePos() {
        return pos;
    }

    @Override
    public void tick() {
        super.tick();
        if (--refreshCountdown > 0) {
            return;
        }
        refreshCountdown = REFRESH_INTERVAL_TICKS;
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        GtMachineSnapshots.at(level, pos).ifPresent(next -> {
            this.snapshot = next;
            setRoot(buildRoot(next, pos));
        });
    }
    private static GtMachineSnapshot peek(BlockPos pos) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return GtMachineSnapshot.empty();
        }
        return GtMachineSnapshots.at(level, pos).orElse(GtMachineSnapshot.empty());
    }

    private static Widget buildRoot(GtMachineSnapshot snapshot, BlockPos pos) {
        TextMetrics metrics = fontMetrics();
        Stack root = Stack.horizontal().padding(Insets.all(8))
                .align(MainAxisAlign.CENTER, CrossAxisAlign.CENTER);
        Stack column = root.add(Stack.vertical().gap(4)
                .align(MainAxisAlign.CENTER, CrossAxisAlign.CENTER));
        MachineStatusPanel panel = new MachineStatusPanel(snapshot, metrics, ItemStackIcon::of);
        panel.fixedSize(280, 160);
        column.add(panel);
        column.add(new TextWidget("GTSN UI · 只读机器状态 · " + pos.toShortString(), metrics)
                .colorRole(ThemeColorRole.TEXT_MUTED));
        return root;
    }

    private static TextMetrics fontMetrics() {
        return new TextMetrics() {
            @Override
            public int width(String text) {
                return Minecraft.getInstance().font.width(text);
            }

            @Override
            public int lineHeight() {
                return Minecraft.getInstance().font.lineHeight;
            }
        };
    }
}
