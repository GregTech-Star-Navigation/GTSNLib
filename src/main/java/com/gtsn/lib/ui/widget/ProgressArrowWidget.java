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
 * 配方进度箭头：GT 机器配方进度（progress / maxProgress）的向右箭头比例填充。
 *
 * <p>状态逻辑（进度钳制、比例映射、工作态）与渲染解耦，可在无游戏环境测试。箭头以纯矩形
 * 拼出（不依赖字体 / 纹理）；未填充部分取轨道色，已填充部分按比例裁剪后取填充色。</p>
 *
 * <p>工作态（{@link #working(boolean)}）：{@code true} 用完整填充色；{@code false}（空闲 / 暂停）
 * 把填充色按 {@value #IDLE_ALPHA_SCALE} 降低 alpha，呈现「变暗」的空闲视觉，避免把暂停态误读为运行中。</p>
 */
public final class ProgressArrowWidget extends AbstractWidget {

    /** 空闲态填充色 alpha 缩放（{@code false} 时）。 */
    private static final float IDLE_ALPHA_SCALE = 0.5f;

    private int progress;
    private int maxProgress;
    private boolean working;

    private ThemeColor background = ThemeColor.role(ThemeColorRole.PROGRESS_TRACK);
    private ThemeColor fillColor = ThemeColor.role(ThemeColorRole.ACCENT);
    private ThemeColor borderColor = ThemeColor.role(ThemeColorRole.BORDER);
    private int borderWidth = 0;

    public ProgressArrowWidget() {
        node().contentMeasurer((widthSpec, heightSpec) ->
                Size.of(widthSpec.resolve(22), heightSpec.resolve(16)));
    }

    /** 设置进度与总量；负值归零，进度不超过总量。 */
    public ProgressArrowWidget progress(int progress, int maxProgress) {
        this.maxProgress = Math.max(0, maxProgress);
        int normalized = Math.max(0, progress);
        this.progress = this.maxProgress > 0 ? Math.min(normalized, this.maxProgress) : 0;
        return this;
    }

    public int progress() {
        return progress;
    }

    public int maxProgress() {
        return maxProgress;
    }

    public boolean working() {
        return working;
    }

    public ProgressArrowWidget working(boolean working) {
        this.working = working;
        return this;
    }

    /** 进度比例 [0, 1]；总量为 0 时返回 0。 */
    public double ratio() {
        if (maxProgress <= 0) {
            return 0.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) progress / (double) maxProgress));
    }

    /** 字面量覆盖（不随主题变化）。 */
    public ProgressArrowWidget colors(int background, int fillColor, int borderColor) {
        this.background = ThemeColor.literal(background);
        this.fillColor = ThemeColor.literal(fillColor);
        this.borderColor = ThemeColor.literal(borderColor);
        return this;
    }

    public ProgressArrowWidget borderWidth(int pixels) {
        if (pixels < 0) {
            throw new IllegalArgumentException("border width must be non-negative: " + pixels);
        }
        this.borderWidth = pixels;
        return this;
    }

    public ProgressArrowWidget fixedSize(int width, int height) {
        node().params().size(Sizing.fixed(width), Sizing.fixed(height));
        return this;
    }

    public ProgressArrowWidget size(Sizing width, Sizing height) {
        node().params().size(width, height);
        return this;
    }

    public ProgressArrowWidget margin(Insets margin) {
        node().params().margin(margin);
        return this;
    }

    public ProgressArrowWidget weight(float weight) {
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
        context.fill(box.x(), box.y(), box.width(), box.height(), background.resolve(theme));

        int thickness = Math.min(borderWidth, Math.min(box.width(), box.height()));
        int border = borderColor.resolve(theme);
        if (thickness > 0 && (border >>> 24) != 0) {
            context.fill(box.x(), box.y(), box.width(), thickness, border);
            context.fill(box.x(), box.bottom() - thickness, box.width(), thickness, border);
            context.fill(box.x(), box.y() + thickness, thickness, box.height() - 2 * thickness, border);
            context.fill(box.right() - thickness, box.y() + thickness, thickness,
                    box.height() - 2 * thickness, border);
        }

        Rect inner = box.inset(Insets.all(thickness));
        drawArrow(context, inner, borderColor.resolve(theme));
        int fillWidth = (int) Math.round(ratio() * box.width());
        if (fillWidth > 0) {
            context.pushClip(box.x(), box.y(), fillWidth, box.height());
            drawArrow(context, inner, fillColor(theme));
            context.popClip();
        }
    }

    /** 填充色：工作中用完整色，空闲态降低 alpha。 */
    private int fillColor(Theme theme) {
        int argb = fillColor.resolve(theme);
        return working ? argb : scaleAlpha(argb, IDLE_ALPHA_SCALE);
    }

    private static int scaleAlpha(int argb, float scale) {
        int alpha = Math.round(((argb >>> 24) & 0xFF) * scale);
        return (Math.max(0, Math.min(255, alpha)) << 24) | (argb & 0x00FFFFFF);
    }

    /** 以矩形拼出右向箭头（箭杆 + 三角箭头），不依赖字体 / 纹理。 */
    private static void drawArrow(RenderContext context, Rect inner, int color) {
        int width = inner.width();
        int height = inner.height();
        if (width <= 0 || height <= 0) {
            return;
        }
        int shaftWidth = Math.max(1, (int) Math.round(width * 0.6));
        int shaftHeight = Math.max(1, height / 3);
        int centerY = inner.y() + height / 2;
        context.fill(inner.x(), centerY - shaftHeight / 2, shaftWidth, shaftHeight, color);

        int headWidth = width - shaftWidth;
        for (int i = 0; i < headWidth; i++) {
            int columnHeight = Math.max(1, height - (2 * height * i) / Math.max(1, headWidth));
            context.fill(inner.x() + shaftWidth + i, centerY - columnHeight / 2, 1, columnHeight, color);
        }
    }
}
