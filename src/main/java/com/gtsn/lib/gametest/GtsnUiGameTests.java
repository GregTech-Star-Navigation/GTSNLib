package com.gtsn.lib.gametest;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.ui.demo.DemoContent;
import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.input.Keys;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.render.TextureRef;
import com.gtsn.lib.ui.screen.WidgetHost;
import com.gtsn.lib.ui.widget.PlainTextMetrics;
import com.gtsn.lib.ui.widget.Widget;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * UI 内核在真实加载环境（runGameTestServer，专职服务端）中的无渲染行为验证：
 * 布局几何、输入派发、焦点、演示界面装配与 MC-free 渲染管线。
 *
 * <p>这些用例证明布局/输入/渲染抽象在不依赖 Minecraft 客户端类的情况下可用（类加载纪律）。</p>
 */
@GameTestHolder(GTSNLib.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GtsnUiGameTests {

    private static final int SCREEN_WIDTH = 640;
    private static final int SCREEN_HEIGHT = 360;

    private GtsnUiGameTests() {
    }

    /** 已布局完成的演示界面夹具。 */
    private record Fixture(DemoContent demo, WidgetHost host) {
    }

    /** 记录调用次数的 MC-free 渲染上下文。 */
    private static final class CountingRenderContext implements RenderContext {
        private int fills;
        private int texts;
        private int clipsPushed;
        private int clipsPopped;

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
            fills++;
        }

        @Override
        public void fillGradient(int x, int y, int width, int height, int argbTop, int argbBottom) {
            fills++;
        }

        @Override
        public void text(String text, int x, int y, int argb, boolean shadow) {
            texts++;
        }

        @Override
        public void blit(TextureRef texture, int x, int y, int width, int height, int u, int v,
                         int textureWidth, int textureHeight) {
            // 演示界面不绘制纹理。
        }

        @Override
        public void pushClip(int x, int y, int width, int height) {
            clipsPushed++;
        }

        @Override
        public void popClip() {
            clipsPopped++;
        }

        @Override
        public void pushTranslate(float deltaX, float deltaY) {
        }

        @Override
        public void popTranslate() {
        }
    }

    private static Fixture fixture() {
        DemoContent demo = DemoContent.build(PlainTextMetrics.INSTANCE);
        WidgetHost host = new WidgetHost(demo.root());
        host.resize(SCREEN_WIDTH, SCREEN_HEIGHT);
        return new Fixture(demo, host);
    }

    private static void click(WidgetHost host, Widget widget) {
        Rect bounds = widget.bounds();
        double x = bounds.x() + bounds.width() / 2.0;
        double y = bounds.y() + bounds.height() / 2.0;
        host.dispatch(new InputEvent.MousePressed(x, y, 0));
        host.dispatch(new InputEvent.MouseReleased(x, y, 0));
    }

    @GameTest(template = "empty")
    public static void demoTreeLaysOutWithinScreen(GameTestHelper helper) {
        Fixture fixture = fixture();
        DemoContent demo = fixture.demo();
        Widget root = demo.root();

        if (!Rect.of(0, 0, SCREEN_WIDTH, SCREEN_HEIGHT).equals(root.bounds())) {
            helper.fail("demo root bounds were " + root.bounds());
            return;
        }
        for (Widget widget : new Widget[]{demo.clickButton(), demo.toggleButton(), demo.clearButton(),
                demo.keypad(), demo.clipShowcase()}) {
            Rect bounds = widget.bounds();
            if (bounds.isEmpty() || !root.bounds().contains(bounds.x(), bounds.y())
                    || bounds.right() > root.bounds().right() || bounds.bottom() > root.bounds().bottom()) {
                helper.fail("widget out of screen bounds: " + bounds);
                return;
            }
        }
        Rect content = root.node().contentBounds();
        Rect badge = demo.badge().bounds();
        if (badge.x() != content.right() - badge.width() - 4 || badge.y() != content.bottom() - badge.height() - 4) {
            helper.fail("badge not anchored bottom-right: badge=" + badge + " content=" + content);
            return;
        }
        if (!hasOverflowingChild(demo.clipShowcase(), demo.clipShowcase().bounds().width())) {
            helper.fail("clip showcase has no overflowing child");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void demoClickUpdatesStateAndStatus(GameTestHelper helper) {
        Fixture fixture = fixture();
        DemoContent demo = fixture.demo();

        click(fixture.host(), demo.clickButton());

        if (demo.state().clicks() != 1) {
            helper.fail("click count was " + demo.state().clicks());
            return;
        }
        if (!demo.clickStatus().text().contains("1")) {
            helper.fail("click status text was " + demo.clickStatus().text());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void demoKeypadReceivesKeyboardAfterFocus(GameTestHelper helper) {
        Fixture fixture = fixture();
        DemoContent demo = fixture.demo();
        WidgetHost host = fixture.host();

        click(host, demo.keypad());
        if (!host.router().focus().isFocused(demo.keypad())) {
            helper.fail("keypad did not receive focus on click");
            return;
        }
        host.dispatch(new InputEvent.KeyPressed('K', 0, 0));
        host.dispatch(new InputEvent.CharTyped('x', 0));
        if (!"K".equals(demo.state().lastKey()) || !"x".equals(demo.state().typed())) {
            helper.fail("keypad state was lastKey=" + demo.state().lastKey() + " typed=" + demo.state().typed());
            return;
        }

        click(host, demo.clearButton());
        if (!"(none)".equals(demo.state().lastKey()) || !demo.state().typed().isEmpty()) {
            helper.fail("clear button did not reset keypad state");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void tabCyclesFocusAcrossDemoButtons(GameTestHelper helper) {
        Fixture fixture = fixture();
        DemoContent demo = fixture.demo();
        WidgetHost host = fixture.host();

        host.dispatch(new InputEvent.KeyPressed(Keys.TAB, 0, 0));
        if (!host.router().focus().isFocused(demo.clickButton())) {
            helper.fail("first Tab did not focus the first button");
            return;
        }
        host.dispatch(new InputEvent.KeyPressed(Keys.TAB, 0, 0));
        if (!host.router().focus().isFocused(demo.toggleButton())) {
            helper.fail("second Tab did not advance focus");
            return;
        }
        host.dispatch(new InputEvent.KeyPressed(Keys.TAB, 0, Keys.MOD_SHIFT));
        if (!host.router().focus().isFocused(demo.clickButton())) {
            helper.fail("Shift+Tab did not move focus backwards");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void demoRendersThroughMcFreeContext(GameTestHelper helper) {
        Fixture fixture = fixture();
        CountingRenderContext context = new CountingRenderContext();

        fixture.host().render(context);

        if (context.fills <= 0 || context.texts <= 0) {
            helper.fail("render produced fills=" + context.fills + " texts=" + context.texts);
            return;
        }
        if (context.clipsPushed < 2 || context.clipsPushed != context.clipsPopped) {
            helper.fail("clip ops were pushed=" + context.clipsPushed + " popped=" + context.clipsPopped);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void layoutIsDeterministicOnServer(GameTestHelper helper) {
        Fixture first = fixture();
        Fixture second = fixture();

        if (!first.demo().clickButton().bounds().equals(second.demo().clickButton().bounds())
                || !first.demo().keypad().bounds().equals(second.demo().keypad().bounds())
                || !first.demo().badge().bounds().equals(second.demo().badge().bounds())) {
            helper.fail("layout is not deterministic between identical builds");
            return;
        }
        helper.succeed();
    }

    private static boolean hasOverflowingChild(Widget widget, int width) {
        for (Widget child : widget.children()) {
            if (child.bounds().width() > width || hasOverflowingChild(child, width)) {
                return true;
            }
        }
        return false;
    }
}
