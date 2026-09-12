package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;

/**
 * 复选框控件：方形勾选框 + 标签，点击/键盘切换选中状态。
 */
public final class CheckboxWidget extends AbstractToggleWidget {

    private int boxSize = 12;
    private int gap = 4;
    private int boxBackground = 0xFF1E1E1E;
    private int borderColor = 0xFF8A8A8A;
    private int hoverBorderColor = 0xFFB8C8D8;
    private int checkColor = 0xFF6FD08A;
    private int textColor = 0xFFE6E6E6;
    private int disabledTextColor = 0xFF8A8A8A;

    public CheckboxWidget(String label, TextMetrics metrics, BooleanConsumer onChange) {
        super(label, metrics, onChange);
        node().contentMeasurer((widthSpec, heightSpec) -> {
            int width = boxSize + gap + metrics.width(this.label);
            int height = Math.max(boxSize, metrics.lineHeight());
            return Size.of(widthSpec.resolve(width), heightSpec.resolve(height));
        });
    }

    public CheckboxWidget boxSize(int boxSize) {
        if (boxSize < 4) {
            throw new IllegalArgumentException("box size must be >= 4: " + boxSize);
        }
        this.boxSize = boxSize;
        return this;
    }

    public int boxSize() {
        return boxSize;
    }

    public CheckboxWidget gap(int gap) {
        if (gap < 0) {
            throw new IllegalArgumentException("gap must be non-negative: " + gap);
        }
        this.gap = gap;
        return this;
    }

    public CheckboxWidget colors(int boxBackground, int borderColor, int checkColor, int textColor) {
        this.boxBackground = boxBackground;
        this.borderColor = borderColor;
        this.checkColor = checkColor;
        this.textColor = textColor;
        return this;
    }

    public CheckboxWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public CheckboxWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public CheckboxWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public CheckboxWidget weight(float weight) {
        node().params().weight(weight);
        return this;
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        if (box.isEmpty()) {
            return;
        }
        int size = Math.min(boxSize, Math.min(box.width(), box.height()));
        int boxY = box.y() + Math.max(0, (box.height() - size) / 2);
        Rect boxRect = Rect.of(box.x(), boxY, size, size);
        context.fill(boxRect.x(), boxRect.y(), size, size, enabled ? boxBackground : 0xFF242424);
        drawBorder(context, boxRect, 1, !enabled ? 0xFF555555 : hovered || pressed ? hoverBorderColor : borderColor);
        if (checked) {
            drawCheck(context, boxRect);
        }
        int textX = box.x() + size + gap;
        int textY = centeredY(box, context.textLineHeight());
        context.text(label, textX, textY, enabled ? textColor : disabledTextColor, false);
    }

    /** 用 4 个短填充近似勾勒对勾（不依赖字体字形）。 */
    private void drawCheck(RenderContext context, Rect box) {
        int unit = Math.max(1, box.width() / 6);
        int x = box.x() + unit;
        int y = box.y();
        context.fill(x, y + 4 * unit, unit, unit, checkColor);
        context.fill(x + unit, y + 5 * unit, unit, unit, checkColor);
        context.fill(x + 2 * unit, y + 3 * unit, unit, unit, checkColor);
        context.fill(x + 3 * unit, y + unit, unit, unit, checkColor);
    }
}
