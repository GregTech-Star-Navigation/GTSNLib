package com.gtsn.lib.gametest;

import com.gtsn.lib.GTSNLib;
import com.gtsn.lib.ui.demo.DemoContent;
import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.render.RenderContext;
import com.gtsn.lib.ui.render.TextureRef;
import com.gtsn.lib.ui.screen.WidgetHost;
import com.gtsn.lib.ui.widget.ButtonState;
import com.gtsn.lib.ui.widget.PlainTextMetrics;
import com.gtsn.lib.ui.widget.Widget;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/**
 * UI 组件库在真实加载环境（runGameTestServer，专职服务端）中的行为验证：
 * 复选框/开关切换、进度钳制、物品槽选择、滚动与工具提示、禁用控件与 MC-free 渲染。
 *
 * <p>这些用例证明组件状态机与渲染抽象不依赖 Minecraft 客户端类（类加载纪律）。</p>
 */
@GameTestHolder(GTSNLib.MOD_ID)
@PrefixGameTestTemplate(false)
public final class GtsnUiComponentsGameTests {

    private static final int SCREEN_WIDTH = 640;
    private static final int SCREEN_HEIGHT = 360;

    private GtsnUiComponentsGameTests() {
    }

    private record Fixture(DemoContent demo, WidgetHost host) {
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

    @GameTest(template = "empty")
    public static void galleryRendersAllComponentKindsOnServer(GameTestHelper helper) {
        Fixture fixture = fixture();
        CountingRenderContext context = new CountingRenderContext();

        fixture.host().render(context);

        if (context.fills < 50 || context.texts < 10) {
            helper.fail("gallery render too sparse: fills=" + context.fills + " texts=" + context.texts);
            return;
        }
        if (context.clipsPushed != 2 || context.clipsPopped != 2) {
            helper.fail("two clip containers expected: pushed=" + context.clipsPushed
                    + " popped=" + context.clipsPopped);
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void checkboxAndSwitchToggleOnServer(GameTestHelper helper) {
        Fixture fixture = fixture();
        DemoContent demo = fixture.demo();

        click(fixture.host(), demo.checkbox());
        click(fixture.host(), demo.toggleSwitch());

        if (!demo.checkbox().checked()) {
            helper.fail("checkbox did not toggle");
            return;
        }
        if (!demo.toggleSwitch().checked()) {
            helper.fail("switch did not toggle");
            return;
        }
        if (!demo.logCheckbox().checked()) {
            helper.fail("preconfigured checked checkbox was not preserved");
            return;
        }
        if (demo.disabledToggle().isEnabled() || !demo.disabledToggle().checked()) {
            helper.fail("disabled toggle configuration was not preserved");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void progressButtonsAdvanceAndClampOnServer(GameTestHelper helper) {
        Fixture fixture = fixture();
        DemoContent demo = fixture.demo();

        click(fixture.host(), demo.progressUpButton());
        click(fixture.host(), demo.progressUpButton());
        if (Math.abs(demo.state().progress() - 0.2) > 1e-9
                || Math.abs(demo.progressBar().progress() - 0.2) > 1e-9) {
            helper.fail("progress after two steps was " + demo.state().progress()
                    + "/" + demo.progressBar().progress());
            return;
        }

        for (int i = 0; i < 12; i++) {
            click(fixture.host(), demo.progressUpButton());
        }
        if (demo.state().progress() != 1.0 || demo.progressBar().progress() != 1.0) {
            helper.fail("progress was not clamped to 1: " + demo.state().progress());
            return;
        }

        click(fixture.host(), demo.progressResetButton());
        if (demo.state().progress() != 0.0 || demo.progressBar().progress() != 0.0) {
            helper.fail("progress reset failed: " + demo.state().progress());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemSlotSelectionIsExclusiveOnServer(GameTestHelper helper) {
        Fixture fixture = fixture();
        DemoContent demo = fixture.demo();

        click(fixture.host(), demo.itemSlot1());
        if (!demo.itemSlot1().isSelected()) {
            helper.fail("slot 1 was not selected");
            return;
        }

        click(fixture.host(), demo.itemSlot2());
        if (!demo.itemSlot2().isSelected() || demo.itemSlot1().isSelected()) {
            helper.fail("slot selection was not mutually exclusive");
            return;
        }
        if (!demo.selectionStatus().text().contains("2")) {
            helper.fail("selection status text was " + demo.selectionStatus().text());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void scrollPanelWheelScrollsAndClampsOnServer(GameTestHelper helper) {
        Fixture fixture = fixture();
        DemoContent demo = fixture.demo();
        Rect viewport = demo.scrollPanel().viewportBounds();

        int before = demo.scrollPanel().scrollY();
        fixture.host().dispatch(new InputEvent.MouseScrolled(viewport.x() + 8, viewport.y() + 8, 0, -2));
        if (demo.scrollPanel().scrollY() <= before) {
            helper.fail("wheel did not scroll down: " + demo.scrollPanel().scrollY());
            return;
        }

        for (int i = 0; i < 20; i++) {
            fixture.host().dispatch(new InputEvent.MouseScrolled(viewport.x() + 8, viewport.y() + 8, 0, -2));
        }
        if (demo.scrollPanel().scrollY() != demo.scrollPanel().maxScroll()) {
            helper.fail("scroll did not clamp at max: " + demo.scrollPanel().scrollY()
                    + "/" + demo.scrollPanel().maxScroll());
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void itemSlotTooltipResolvesOnServer(GameTestHelper helper) {
        Fixture fixture = fixture();
        DemoContent demo = fixture.demo();
        Rect slot = demo.itemSlot1().bounds();

        fixture.host().dispatch(new InputEvent.MouseMoved(slot.x() + 4, slot.y() + 4));
        if (fixture.host().activeTooltip().isEmpty()) {
            helper.fail("tooltip did not resolve while hovering the item slot");
            return;
        }
        fixture.host().dispatch(new InputEvent.MouseMoved(1, SCREEN_HEIGHT - 1));
        if (fixture.host().activeTooltip().isPresent()) {
            helper.fail("tooltip did not clear after leaving the slot");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void disabledControlsIgnoreInputOnServer(GameTestHelper helper) {
        Fixture fixture = fixture();
        DemoContent demo = fixture.demo();

        if (demo.disabledButton().state() != ButtonState.DISABLED) {
            helper.fail("disabled button state was " + demo.disabledButton().state());
            return;
        }
        int clicks = demo.state().clicks();
        click(fixture.host(), demo.disabledButton());
        click(fixture.host(), demo.disabledItemSlot());
        if (demo.state().clicks() != clicks) {
            helper.fail("disabled button fired: " + demo.state().clicks());
            return;
        }
        if (demo.disabledItemSlot().isSelected()) {
            helper.fail("disabled item slot was selected");
            return;
        }
        if (!demo.disabledToggle().checked()) {
            helper.fail("disabled toggle state changed");
            return;
        }
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void galleryLayoutIsDeterministicOnServer(GameTestHelper helper) {
        Fixture first = fixture();
        Fixture second = fixture();

        if (!first.demo().checkbox().bounds().equals(second.demo().checkbox().bounds())
                || !first.demo().progressBar().bounds().equals(second.demo().progressBar().bounds())
                || !first.demo().itemSlot1().bounds().equals(second.demo().itemSlot1().bounds())
                || !first.demo().scrollPanel().bounds().equals(second.demo().scrollPanel().bounds())) {
            helper.fail("gallery layout is not deterministic between identical builds");
            return;
        }
        helper.succeed();
    }
}
