package com.gtsn.lib.ui.widget;

import com.gtsn.lib.ui.layout.Anchor;
import com.gtsn.lib.ui.layout.CrossAxisAlign;
import com.gtsn.lib.ui.layout.Insets;
import com.gtsn.lib.ui.layout.MainAxisAlign;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.layout.Sizing;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.render.TextureRef;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeColor;
import com.gtsn.lib.ui.theme.ThemeColorRole;
import com.gtsn.lib.ui.theme.ThemeId;
import com.gtsn.lib.ui.theme.ThemeTextureRole;

import java.util.Objects;

/**
 * 面板容器：背景 + 边框 + 可选标题头的分组容器，子控件按栈布局排列。
 *
 * <p>与 {@link Stack} 的区别在于自带容器绘制与标题头；标题占用头部高度，
 * 内容区自动下移（用户 padding 之上叠加头部高度）。</p>
 *
 * <p>主题（#18）：{@link #background(ThemeColorRole)} / {@link #border(ThemeColorRole, int)}
 * 声明角色引用，渲染时按当前主题解析；角色背景同时启用主题面板纹理（存在时平铺，
 * 缺失时回退纯色）。默认不绘制背景 / 边框（纯布局容器，与既有契约一致）。</p>
 */
public final class PanelWidget extends AbstractWidget {

    /** 面板纹理的平铺尺寸（与随附纹理资源一致）。 */
    private static final int PANEL_TILE = 16;

    private ThemeColor background;
    private ThemeColor borderColor;
    private int borderWidth = 1;
    private String title;
    private TextMetrics titleMetrics;
    private ThemeColor titleColor = ThemeColor.role(ThemeColorRole.TEXT_STRONG);
    private ThemeColor headerFill = ThemeColor.role(ThemeColorRole.PANEL_HEADER);
    private ThemeColor headerRule = ThemeColor.role(ThemeColorRole.PANEL_HEADER_RULE);
    private Insets userPadding = Insets.NONE;

    public PanelWidget() {
    }

    /** 显式背景字面量（不随主题变化）。 */
    public PanelWidget background(int argb) {
        this.background = ThemeColor.literal(argb);
        return this;
    }

    /** 背景取主题语义角色（渲染时解析；启用主题面板纹理）。 */
    public PanelWidget background(ThemeColorRole role) {
        this.background = ThemeColor.role(Objects.requireNonNull(role, "role"));
        return this;
    }

    /** 显式边框字面量。 */
    public PanelWidget border(int argb, int thickness) {
        this.borderColor = ThemeColor.literal(argb);
        this.borderWidth = Math.max(0, thickness);
        return this;
    }

    /** 边框取主题语义角色。 */
    public PanelWidget border(ThemeColorRole role, int thickness) {
        this.borderColor = ThemeColor.role(Objects.requireNonNull(role, "role"));
        this.borderWidth = Math.max(0, thickness);
        return this;
    }

    public PanelWidget title(String title, TextMetrics metrics) {
        this.title = Objects.requireNonNull(title, "title");
        this.titleMetrics = Objects.requireNonNull(metrics, "metrics");
        applyPadding();
        return this;
    }

    public String title() {
        return title;
    }

    public PanelWidget titleColor(int argb) {
        this.titleColor = ThemeColor.literal(argb);
        return this;
    }

    public PanelWidget headerColors(int fill, int rule) {
        this.headerFill = ThemeColor.literal(fill);
        this.headerRule = ThemeColor.literal(rule);
        return this;
    }

    public PanelWidget padding(Insets padding) {
        this.userPadding = Objects.requireNonNull(padding, "padding");
        applyPadding();
        return this;
    }

    public PanelWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public PanelWidget gap(int gap) {
        node().params().gap(gap);
        return this;
    }

    public PanelWidget direction(com.gtsn.lib.ui.layout.Direction direction) {
        node().params().direction(direction);
        return this;
    }

    public PanelWidget align(MainAxisAlign main, CrossAxisAlign cross) {
        node().params().mainAxisAlign(main).crossAxisAlign(cross);
        return this;
    }

    public PanelWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public PanelWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public PanelWidget fill() {
        node().params().fill();
        return this;
    }

    public PanelWidget fillWidth() {
        node().params().fillWidth();
        return this;
    }

    public PanelWidget weight(float weight) {
        node().params().weight(weight);
        return this;
    }

    public PanelWidget absolute(Anchor anchor, int offsetX, int offsetY) {
        node().params().absolute(anchor, offsetX, offsetY);
        return this;
    }

    /** 标题头高度（无标题为 0）。 */
    public int headerHeight() {
        return title == null ? 0 : titleMetrics.lineHeight() + 6;
    }

    @Override
    protected void onRender(RenderContext context) {
        Rect box = bounds();
        int width = box.width();
        int height = box.height();
        if (width <= 0 || height <= 0) {
            return;
        }
        Theme theme = context.theme();
        if (background != null) {
            int argb = background.resolve(theme);
            boolean textured = false;
            if (background.isRole()) {
                ThemeId texture = theme.texture(ThemeTextureRole.PANEL);
                if (texture != null) {
                    TextureRef ref = TextureRef.of(texture.namespace(), texture.path());
                    if (context.textureReady(ref)) {
                        if ((argb >>> 24) != 0) {
                            context.fill(box.x(), box.y(), width, height, argb);
                        }
                        context.blitTiled(ref, box.x(), box.y(), width, height, PANEL_TILE);
                        textured = true;
                    }
                }
            }
            if (!textured && (argb >>> 24) != 0) {
                context.fill(box.x(), box.y(), width, height, argb);
            }
        }
        if (borderColor != null && borderWidth > 0) {
            int argb = borderColor.resolve(theme);
            if ((argb >>> 24) != 0) {
                int thickness = Math.min(borderWidth, Math.min(width, height));
                context.fill(box.x(), box.y(), width, thickness, argb);
                context.fill(box.x(), box.bottom() - thickness, width, thickness, argb);
                context.fill(box.x(), box.y() + thickness, thickness, height - 2 * thickness, argb);
                context.fill(box.right() - thickness, box.y() + thickness, thickness,
                        height - 2 * thickness, argb);
            }
        }
        if (title != null) {
            int header = Math.min(headerHeight(), height);
            int headerTop = box.y() + userPadding.top();
            context.fill(box.x(), headerTop, width, header, headerFill.resolve(theme));
            if (header > 0) {
                context.fill(box.x(), headerTop + header - 1, width, 1, headerRule.resolve(theme));
            }
            int textY = headerTop + Math.max(0, (header - context.textLineHeight()) / 2);
            context.text(title, box.x() + userPadding.left(), textY, titleColor.resolve(theme), false);
        }
    }

    private void applyPadding() {
        int header = headerHeight();
        node().params().padding(
                new Insets(userPadding.top() + header, userPadding.right(), userPadding.bottom(), userPadding.left()));
    }
}
