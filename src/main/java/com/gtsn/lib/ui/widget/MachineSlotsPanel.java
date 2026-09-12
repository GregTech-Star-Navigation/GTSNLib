package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.SlotIcon;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 机器槽位面板：把一组 {@link SlotIcon} 按固定列数排成网格（每格一个 {@link ItemSlotWidget}）。
 *
 * <p>本控件不持有物品 / 流体类型——调用方（适配层 / 界面）负责把 {@code ItemStack} 翻译为
 * {@code SlotIcon}（客户端用 {@code ItemStackIcon}），从而保持组件与 GT / MC 类型解耦、可无游戏环境测试。
 * 空槽位用 {@code null} 图标表示。网格整体作为单一子控件挂载，便于替换 / 重建。</p>
 */
public final class MachineSlotsPanel extends AbstractWidget {

    private static final int DEFAULT_SLOT_GAP = 2;

    private final List<ItemSlotWidget> slots = new ArrayList<>();
    private final List<SlotIcon> icons;
    private final int columns;
    private final int rows;

    public MachineSlotsPanel(List<SlotIcon> icons, int columns) {
        Objects.requireNonNull(icons, "icons");
        if (columns < 1) {
            throw new IllegalArgumentException("columns must be >= 1: " + columns);
        }
        this.icons = Collections.unmodifiableList(new ArrayList<>(icons));
        this.columns = columns;
        this.rows = icons.isEmpty() ? 0 : (icons.size() + columns - 1) / columns;

        Stack grid = Stack.vertical().gap(DEFAULT_SLOT_GAP);
        for (int row = 0; row < rows; row++) {
            Stack rowStack = Stack.horizontal().gap(DEFAULT_SLOT_GAP);
            for (int column = 0; column < columns; column++) {
                int index = row * columns + column;
                if (index >= icons.size()) {
                    break;
                }
                SlotIcon icon = icons.get(index);
                ItemSlotWidget slot = new ItemSlotWidget();
                if (icon != null) {
                    slot.icon(icon);
                }
                slots.add(slot);
                rowStack.add(slot);
            }
            grid.add(rowStack);
        }
        add(grid);
    }

    public int slotCount() {
        return slots.size();
    }

    public int columns() {
        return columns;
    }

    public int rows() {
        return rows;
    }

    /** 第 {@code index} 个槽位的图标（可能为 {@code null}）。 */
    public SlotIcon icon(int index) {
        return icons.get(index);
    }

    /** 第 {@code index} 个槽位控件；越界抛 {@link IndexOutOfBoundsException}。 */
    public ItemSlotWidget slot(int index) {
        return slots.get(index);
    }

    public MachineSlotsPanel gap(int gap) {
        node().params().gap(gap);
        return this;
    }

    public MachineSlotsPanel padding(Insets padding) {
        node().params().padding(padding);
        return this;
    }

    public MachineSlotsPanel size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public MachineSlotsPanel fillWidth() {
        node().params().fillWidth();
        return this;
    }

    public MachineSlotsPanel weight(float weight) {
        node().params().weight(weight);
        return this;
    }
}
