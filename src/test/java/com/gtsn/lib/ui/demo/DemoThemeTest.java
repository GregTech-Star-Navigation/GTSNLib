package com.gtsn.lib.ui.demo;

import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.screen.WidgetHost;
import com.gtsn.lib.ui.theme.Spacing;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeContext;
import com.gtsn.lib.ui.theme.ThemeDefinition;
import com.gtsn.lib.ui.theme.ThemeId;
import com.gtsn.lib.ui.theme.ThemeResolver;
import com.gtsn.lib.ui.widget.PlainTextMetrics;
import com.gtsn.lib.ui.widget.Widget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 开发测试界面的主题接线：切换按钮与 {@link ThemeControl} 的联动、
 * 无控制时按钮禁用、主题间距刻度驱动装配尺寸。
 */
class DemoThemeTest {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;

    /** 记录调用并切换展示名的假控制。 */
    private static final class FakeThemeControl implements ThemeControl {

        private int nextCalls;
        private String name = "主题-A";

        @Override
        public String activeThemeName() {
            return name;
        }

        @Override
        public void nextTheme() {
            nextCalls++;
            name = "主题-B";
        }
    }

    private static void click(WidgetHost host, Widget widget) {
        Rect bounds = widget.bounds();
        double x = bounds.x() + bounds.width() / 2.0;
        double y = bounds.y() + bounds.height() / 2.0;
        host.dispatch(new InputEvent.MousePressed(x, y, 0));
        host.dispatch(new InputEvent.MouseReleased(x, y, 0));
    }

    private static Theme themeWithSpacing(int lg, int md) {
        ThemeId id = ThemeId.of("gtsnlib", "dense");
        return ThemeResolver.resolve(List.of(ThemeDefinition.builder(id)
                .spacing(Spacing.LG, lg)
                .spacing(Spacing.MD, md)
                .build())).get(id);
    }

    @Test
    void themeButtonShowsControlNameAndCyclesOnClick() {
        FakeThemeControl control = new FakeThemeControl();
        DemoContent demo = DemoContent.build(new DemoState(), PlainTextMetrics.INSTANCE,
                DemoIcons.placeholders(), control, ThemeContext.active());
        WidgetHost host = new WidgetHost(demo.root());
        host.resize(WIDTH, HEIGHT);

        assertTrue(demo.themeButton().label().contains("主题-A"), demo.themeButton().label());

        click(host, demo.themeButton());

        assertEquals(1, control.nextCalls);
        assertTrue(demo.themeButton().label().contains("主题-B"), demo.themeButton().label());
    }

    @Test
    void themeButtonIsDisabledWithoutControl() {
        DemoContent demo = DemoContent.build(PlainTextMetrics.INSTANCE);

        assertFalse(demo.themeButton().isEnabled());
        assertTrue(demo.themeButton().label().contains("未接入"), demo.themeButton().label());
    }

    @Test
    void themeSpacingDrivesGalleryMetrics() {
        Theme dense = themeWithSpacing(4, 3);
        DemoContent base = DemoContent.build(new DemoState(), PlainTextMetrics.INSTANCE,
                DemoIcons.placeholders(), null, ThemeContext.active());
        DemoContent compact = DemoContent.build(new DemoState(), PlainTextMetrics.INSTANCE,
                DemoIcons.placeholders(), null, dense);
        WidgetHost baseHost = new WidgetHost(base.root());
        WidgetHost compactHost = new WidgetHost(compact.root());
        baseHost.resize(WIDTH, HEIGHT);
        compactHost.resize(WIDTH, HEIGHT);

        assertNotEquals(base.clickButton().bounds(), compact.clickButton().bounds(),
                "主题间距刻度应驱动装配尺寸（base=" + base.clickButton().bounds()
                        + " compact=" + compact.clickButton().bounds() + "）");
        assertTrue(compact.clickButton().bounds().x() < base.clickButton().bounds().x(),
                "更小的 LG 间距应把内容推得更靠左上");
    }
}
