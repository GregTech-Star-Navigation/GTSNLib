package com.gtsn.lib.gametest;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.ui.demo.DemoContent;
import com.gtsn.lib.ui.demo.DemoIcons;
import com.gtsn.lib.ui.demo.DemoState;
import com.gtsn.lib.ui.demo.ThemeControl;
import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.render.TextureRef;
import com.gtsn.lib.ui.screen.WidgetHost;
import com.gtsn.lib.ui.theme.Theme;
import com.gtsn.lib.ui.theme.ThemeColorRole;
import com.gtsn.lib.ui.theme.ThemeContext;
import com.gtsn.lib.ui.theme.ThemeDefinition;
import com.gtsn.lib.ui.theme.ThemeId;
import com.gtsn.lib.ui.theme.ThemeParser;
import com.gtsn.lib.ui.theme.ThemeRegistry;
import com.gtsn.lib.ui.theme.ThemeResolver;
import com.gtsn.lib.ui.widget.PlainTextMetrics;
import com.gtsn.lib.ui.widget.Widget;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * UI 主题系统在专职服务端加载环境（runGameTestServer）中的行为验证：
 * 解析 / 继承 / 回退、上下文轮换，以及“同一控件树在不同主题下渲染出不同颜色”的可见性证据。
 *
 * <p>全部为 MC-free 类路径：主题包不引用客户端类（类加载纪律，见 ADR-0003/0004）。</p>
 */
@GameTestHolder(GTSNLib.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GtsnUiThemeGameTests {

    private static final int SCREEN_WIDTH = 640;
    private static final int SCREEN_HEIGHT = 360;

    private GtsnUiThemeGameTests() {
    }

    /** 收集填充色集合的主题渲染上下文。 */
    private static final class FillColorContext implements RenderContext {

        private final Theme theme;
        private final Set<Integer> fillColors = new HashSet<>();

        private FillColorContext(Theme theme) {
            this.theme = theme;
        }

        @Override
        public Theme theme() {
            return theme;
        }

        @Override
        public int width() {
            return SCREEN_WIDTH;
        }

        @Override
        public int height() {
            return SCREEN_HEIGHT;
        }

        @Override
        public int textWidth(String text) {
            return text.length() * 6;
        }

        @Override
        public int textLineHeight() {
            return 9;
        }

        @Override
        public void fill(int x, int y, int width, int height, int argb) {
            fillColors.add(argb);
        }

        @Override
        public void fillGradient(int x, int y, int width, int height, int argbTop, int argbBottom) {
            fillColors.add(argbTop);
            fillColors.add(argbBottom);
        }

        @Override
        public void text(String text, int x, int y, int argb, boolean shadow) {
        }

        @Override
        public void blit(TextureRef texture, int x, int y, int width, int height, int u, int v,
                         int textureWidth, int textureHeight) {
        }

        @Override
        public void pushClip(int x, int y, int width, int height) {
        }

        @Override
        public void popClip() {
        }

        @Override
        public void pushTranslate(float deltaX, float deltaY) {
        }

        @Override
        public void popTranslate() {
        }
    }

    /** 记录切换次数的假主题控制。 */
    private static final class FakeThemeControl implements ThemeControl {

        private int nextCalls;

        @Override
        public String activeThemeName() {
            return nextCalls == 0 ? "甲" : "乙";
        }

        @Override
        public void nextTheme() {
            nextCalls++;
        }
    }

    private static Theme customTheme(String path, ThemeColorRole role, int argb) {
        ThemeId id = ThemeId.of("gtsnlib", path);
        return ThemeResolver.resolve(List.of(
                ThemeDefinition.builder(id).color(role, argb).build())).get(id);
    }

    private static void click(WidgetHost host, Widget widget) {
        Rect bounds = widget.bounds();
        double x = bounds.x() + bounds.width() / 2.0;
        double y = bounds.y() + bounds.height() / 2.0;
        host.dispatch(new InputEvent.MousePressed(x, y, 0));
        host.dispatch(new InputEvent.MouseReleased(x, y, 0));
    }

    @GameTest(template = "empty")
    public static void themeParserAndResolverWorkOnServer(GameTestHelper helper) {
        ThemeId customId = ThemeId.of("gtsnlib", "server-test");
        ThemeDefinition definition = ThemeParser.parse(customId, """
                {
                  "extends": "gtsnlib:default",
                  "colors": { "panel_background": "#FF102030" },
                  "spacing": { "md": 9 }
                }
                """);
        ThemeRegistry registry = ThemeResolver.resolve(List.of(definition));
        Theme theme = registry.get(customId);

        if (theme.color(ThemeColorRole.PANEL_BACKGROUND) != 0xFF102030) {
            helper.fail("explicit color was not applied: " + Integer.toHexString(theme.color(ThemeColorRole.PANEL_BACKGROUND)));
            return;
        }
        if (theme.color(ThemeColorRole.TEXT) != ThemeColorRole.TEXT.defaultArgb()) {
            helper.fail("missing role did not fall back to default: "
                    + Integer.toHexString(theme.color(ThemeColorRole.TEXT)));
            return;
        }
        if (theme.spacing(com.gtsn.lib.ui.theme.Spacing.MD) != 9) {
            helper.fail("spacing override was not applied");
            return;
        }
        if (registry.get(ThemeId.of("gtsnlib", "missing")).id() != ThemeRegistry.DEFAULT_ID) {
            helper.fail("unknown theme did not fall back to default");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void themeContextCyclesAndResetsOnServer(GameTestHelper helper) {
        try {
            ThemeContext.setRegistry(ThemeResolver.resolve(List.of(
                    ThemeDefinition.builder(ThemeRegistry.DEFAULT_ID).build(),
                    ThemeDefinition.builder(ThemeId.of("gtsnlib", "amber")).build(),
                    ThemeDefinition.builder(ThemeId.of("gtsnlib", "light")).build())));

            if (!ThemeContext.setActive(ThemeId.of("gtsnlib", "light"))) {
                helper.fail("setActive(light) failed");
                return;
            }
            if (!ThemeContext.activeId().equals(ThemeId.of("gtsnlib", "light"))) {
                helper.fail("active id was " + ThemeContext.activeId());
                return;
            }
            if (ThemeContext.setActive(ThemeId.of("gtsnlib", "missing"))) {
                helper.fail("unknown theme was accepted");
                return;
            }
            if (!ThemeContext.activeId().equals(ThemeRegistry.DEFAULT_ID)) {
                helper.fail("unknown theme did not fall back to default: " + ThemeContext.activeId());
                return;
            }
            ThemeId next = ThemeContext.cycle();
            if (!next.equals(ThemeId.of("gtsnlib", "amber"))) {
                helper.fail("cycle order was " + next);
                return;
            }
            helper.succeed();
        } finally {
            ThemeContext.reset();
        }
    }

    @GameTest(template = "empty")
    public static void themeSwitchChangesRenderedColorsOnServer(GameTestHelper helper) {
        DemoContent demo = DemoContent.build(PlainTextMetrics.INSTANCE);
        WidgetHost host = new WidgetHost(demo.root());
        host.resize(SCREEN_WIDTH, SCREEN_HEIGHT);

        Theme dark = ThemeRegistry.builtin().defaultTheme();
        Theme light = customTheme("server-light", ThemeColorRole.PANEL_BACKGROUND, 0xFFF4F6FA);

        FillColorContext darkContext = new FillColorContext(dark);
        FillColorContext lightContext = new FillColorContext(light);
        host.render(darkContext);
        host.render(lightContext);

        if (darkContext.fillColors.isEmpty() || lightContext.fillColors.isEmpty()) {
            helper.fail("rendering produced no fills");
            return;
        }
        if (darkContext.fillColors.equals(lightContext.fillColors)) {
            helper.fail("theme switch did not change any rendered fill color");
            return;
        }
        if (!lightContext.fillColors.contains(0xFFF4F6FA)) {
            helper.fail("light theme panel color was not used in rendering");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void demoThemeButtonDrivesThemeControlOnServer(GameTestHelper helper) {
        FakeThemeControl control = new FakeThemeControl();
        DemoContent demo = DemoContent.build(new DemoState(), PlainTextMetrics.INSTANCE,
                DemoIcons.placeholders(), control, ThemeRegistry.builtin().defaultTheme());
        WidgetHost host = new WidgetHost(demo.root());
        host.resize(SCREEN_WIDTH, SCREEN_HEIGHT);

        if (!demo.themeButton().isEnabled()) {
            helper.fail("theme button is disabled despite a wired control");
            return;
        }
        click(host, demo.themeButton());

        if (control.nextCalls != 1) {
            helper.fail("theme control nextTheme calls: " + control.nextCalls);
            return;
        }
        if (!demo.themeButton().label().contains("乙")) {
            helper.fail("theme button label did not update: " + demo.themeButton().label());
            return;
        }
        helper.succeed();
    }
}
