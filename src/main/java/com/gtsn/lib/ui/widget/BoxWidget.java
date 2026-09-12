package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Anchor;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.theme.ThemeColor;
import com.gtsn.lib.ui.theme.ThemeColorRole;

import java.util.Objects;

/**
 * 纯色盒控件：填充 + 可选边框，作为布局演示与容器背景的基础绘制单元。
 *
 * <p>填充默认取主题 {@link ThemeColorRole#BOX_BACKGROUND} 角色；{@link #fill(int)} /
 * {@link #fill(ThemeColorRole)} 覆盖。</p>
 */
public final class BoxWidget extends AbstractWidget {

    private ThemeColor fillColor = ThemeColor.role(ThemeColorRole.BOX_BACKGROUND);
    private ThemeColor borderColor;
    private int borderWidth = 1;

    /** 显式颜色字面量（不随主题变化）。 */
    public BoxWidget fill(int argb) {
        this.fillColor = ThemeColor.literal(argb);
        return this;
    }

    /** 语义颜色角色（渲染时按当前主题解析）。 */
    public BoxWidget fill(ThemeColorRole role) {
        this.fillColor = ThemeColor.role(Objects.requireNonNull(role, "role"));
        return this;
    }

    /** 显式边框字面量。 */
    public BoxWidget border(int argb, int thickness) {
        this.borderColor = ThemeColor.literal(argb);
        this.borderWidth = Math.max(0, thickness);
        return this;
    }

    /** 边框取主题语义角色。 */
    public BoxWidget border(ThemeColorRole role, int thickness) {
        this.borderColor = ThemeColor.role(Objects.requireNonNull(role, "role"));
        this.borderWidth = Math.max(0, thickness);
        return this;
    }

    public BoxWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public BoxWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public BoxWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public BoxWidget padding(Insets padding) {
        node().params().padding(padding);
        return this;
    }

    public BoxWidget weight(float weight) {
        node().params().weight(weight);
        return this;
    }

    public BoxWidget absolute(Anchor anchor, int offsetX, int offsetY) {
        node().params().absolute(anchor, offsetX, offsetY);
        return this;
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        context.fill(box.x(), box.y(), box.width(), box.height(), fillColor.resolve(context.theme()));
        if (borderColor != null && borderWidth > 0 && box.width() > 0 && box.height() > 0) {
            int border = borderColor.resolve(context.theme());
            if ((border >>> 24) == 0) {
                return;
            }
            int thickness = Math.min(borderWidth, Math.min(box.width(), box.height()));
            context.fill(box.x(), box.y(), box.width(), thickness, border);
            context.fill(box.x(), box.bottom() - thickness, box.width(), thickness, border);
            context.fill(box.x(), box.y() + thickness, thickness, box.height() - 2 * thickness, border);
            context.fill(box.right() - thickness, box.y() + thickness, thickness,
                    box.height() - 2 * thickness, border);
        }
    }
}
