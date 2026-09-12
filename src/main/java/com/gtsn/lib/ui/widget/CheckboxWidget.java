package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Size;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeColor;
import com.gtsn.lib.ui.theme.ThemeColorRole;

/**
 * 复选框控件：方形勾选框 + 标签，点击/键盘切换选中状态。
 *
 * <p>颜色默认取主题角色，{@link #colors} 可字面量覆盖。</p>
 */
public final class CheckboxWidget extends AbstractToggleWidget {

    private int boxSize = 12;
    private int gap = 4;
    private ThemeColor boxBackground = ThemeColor.role(ThemeColorRole.CHECKBOX_BACKGROUND);
    private ThemeColor borderColor = ThemeColor.role(ThemeColorRole.BORDER);
    private ThemeColor checkColor = ThemeColor.role(ThemeColorRole.CHECK_MARK);
    private ThemeColor textColor = ThemeColor.role(ThemeColorRole.TEXT);

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

    /** 字面量覆盖（不随主题变化）。 */
    public CheckboxWidget colors(int boxBackground, int borderColor, int checkColor, int textColor) {
        this.boxBackground = ThemeColor.literal(boxBackground);
        this.borderColor = ThemeColor.literal(borderColor);
        this.checkColor = ThemeColor.literal(checkColor);
        this.textColor = ThemeColor.literal(textColor);
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
        Theme theme = context.theme();
        int size = Math.min(boxSize, Math.min(box.width(), box.height()));
        int boxY = box.y() + Math.max(0, (box.height() - size) / 2);
        Rect boxRect = Rect.of(box.x(), boxY, size, size);
        context.fill(boxRect.x(), boxRect.y(), size, size,
                enabled ? boxBackground.resolve(theme) : theme.color(ThemeColorRole.CHECKBOX_DISABLED_BACKGROUND));
        int border = !enabled ? theme.color(ThemeColorRole.BORDER_DISABLED)
                : hovered || pressed ? theme.color(ThemeColorRole.BORDER_HOVERED)
                : borderColor.resolve(theme);
        drawBorder(context, boxRect, 1, border);
        if (checked) {
            drawCheck(context, boxRect, checkColor.resolve(theme));
        }
        int textX = box.x() + size + gap;
        int textY = centeredY(box, context.textLineHeight());
        context.text(label, textX, textY,
                enabled ? textColor.resolve(theme) : theme.color(ThemeColorRole.TEXT_DISABLED), false);
    }

    /** 用 4 个短填充近似勾勒对勾（不依赖字体字形）。 */
    private void drawCheck(RenderContext context, Rect box, int checkColor) {
        int unit = Math.max(1, box.width() / 6);
        int x = box.x() + unit;
        int y = box.y();
        context.fill(x, y + 4 * unit, unit, unit, checkColor);
        context.fill(x + unit, y + 5 * unit, unit, unit, checkColor);
        context.fill(x + 2 * unit, y + 3 * unit, unit, unit, checkColor);
        context.fill(x + 3 * unit, y + unit, unit, unit, checkColor);
    }
}
