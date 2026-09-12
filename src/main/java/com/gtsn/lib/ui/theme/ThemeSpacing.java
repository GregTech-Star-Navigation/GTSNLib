package com.gtsn.lib.ui.theme;

import java.util.Objects;

/**
 * 主题间距刻度（像素）；缺省值与库现有视觉一致（2/4/6/8/10）。
 */
public record ThemeSpacing(int xs, int sm, int md, int lg, int xl) {

    public static final ThemeSpacing DEFAULT = new ThemeSpacing(2, 4, 6, 8, 10);

    public ThemeSpacing {
        if (xs < 0 || sm < 0 || md < 0 || lg < 0 || xl < 0) {
            throw new IllegalArgumentException("spacing must be non-negative");
        }
    }

    public int pixels(Spacing step) {
        return switch (Objects.requireNonNull(step, "step")) {
            case XS -> xs;
            case SM -> sm;
            case MD -> md;
            case LG -> lg;
            case XL -> xl;
        };
    }
}
