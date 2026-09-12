package com.gtsn.lib.ui.demo;

import com.gtsn.lib.ui.RecordingRenderContext;
import com.gtsn.lib.ui.input.InputEvent;
import com.gtsn.lib.ui.layout.Rect;
import com.gtsn.lib.ui.screen.WidgetHost;
import com.gtsn.lib.ui.widget.ButtonState;
import com.gtsn.lib.ui.widget.PlainTextMetrics;
import com.gtsn.lib.ui.widget.Widget;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 组件库开发测试界面装配：布局、锚定、按钮/复选框/开关/进度条/物品槽/滚动/工具提示交互。
 */
class DemoContentTest {

    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;

    private final RecordingRenderContext ctx = new RecordingRenderContext(WIDTH, HEIGHT);
    private DemoContent demo;
    private WidgetHost host;

    @BeforeEach
    void setUp() {
        demo = DemoContent.build(PlainTextMetrics.INSTANCE);
        host = new WidgetHost(demo.root());
        host.resize(WIDTH, HEIGHT);
    }

    private void click(Widget widget) {
        Rect bounds = widget.bounds();
        double x = bounds.x() + bounds.width() / 2.0;
        double y = bounds.y() + bounds.height() / 2.0;
        host.dispatch(new InputEvent.MousePressed(x, y, 0));
        host.dispatch(new InputEvent.MouseReleased(x, y, 0));
    }

    @Test
    void rootFillsScreenAndBadgeAnchorsToBottomRight() {
        assertEquals(Rect.of(0, 0, WIDTH, HEIGHT), demo.root().bounds());
        Rect root = demo.root().node().contentBounds();
        Rect badge = demo.badge().bounds();
        assertEquals(root.right() - badge.width() - 4, badge.x());
        assertEquals(root.bottom() - badge.height() - 4, badge.y());
    }

    @Test
    void everyGalleryWidgetLaysOutWithinRoot() {
        Rect root = demo.root().bounds();
        for (Widget widget : new Widget[]{demo.clickButton(), demo.toggleButton(), demo.clearButton(),
                demo.disabledButton(), demo.progressUpButton(), demo.progressResetButton(),
                demo.checkbox(), demo.logCheckbox(), demo.toggleSwitch(), demo.disabledToggle(),
                demo.progressBar(), demo.itemSlot1(), demo.itemSlot2(), demo.disabledItemSlot(),
                demo.scrollPanel(), demo.clipShowcase(), demo.keypad(), demo.badge(),
                demo.fontCompareButton(), demo.fontSample(),
                demo.clickStatus(), demo.toggleStatus(), demo.selectionStatus()}) {
            Rect bounds = widget.bounds();
            if (bounds.isEmpty() || !root.contains(bounds.x(), bounds.y())
                    || bounds.right() > root.right() || bounds.bottom() > root.bottom()) {
                throw new AssertionError("控件越出根边界: " + bounds);
            }
        }
    }

    @Test
    void fontCompareButtonTogglesSampleBetweenThemeAndVanilla() {
        assertEquals(java.util.Optional.empty(), demo.fontSample().fontOverride(),
                "默认主题字体：样例无逐控件覆盖");
        assertEquals("字体: 主题", demo.fontCompareButton().label());

        click(demo.fontCompareButton());

        assertEquals(com.gtsn.lib.ui.theme.FontId.VANILLA, demo.fontSample().fontOverride().orElseThrow(),
                "切换后为显式原版字体");
        assertEquals("字体: 原版", demo.fontCompareButton().label());

        click(demo.fontCompareButton());

        assertEquals(java.util.Optional.empty(), demo.fontSample().fontOverride(),
                "再次切换恢复跟随主题");
        assertEquals("字体: 主题", demo.fontCompareButton().label());
    }

    @Test
    void clickButtonIncrementsStateAndStatusText() {
        click(demo.clickButton());

        assertEquals(1, demo.state().clicks());
        assertTrue(demo.clickStatus().text().contains("1"), demo.clickStatus().text());
    }

    @Test
    void toggleButtonFlipsStateAndStatusText() {
        click(demo.toggleButton());

        assertTrue(demo.state().toggled());
        assertTrue(demo.toggleStatus().text().contains("B"), demo.toggleStatus().text());
    }

    @Test
    void keypadReceivesKeysAfterClickFocusAndClearResets() {
        click(demo.keypad());
        assertTrue(host.router().focus().isFocused(demo.keypad()));

        host.dispatch(new InputEvent.KeyPressed('K', 0, 0));
        host.dispatch(new InputEvent.CharTyped('x', 0));
        host.dispatch(new InputEvent.CharTyped('y', 0));

        assertEquals("K", demo.state().lastKey());
        assertEquals("xy", demo.state().typed());

        click(demo.clearButton());

        assertEquals("(none)", demo.state().lastKey());
        assertEquals("", demo.state().typed());
    }

    @Test
    void disabledButtonStaysDisabledAndDoesNotFire() {
        assertEquals(ButtonState.DISABLED, demo.disabledButton().state());

        int before = demo.state().clicks();
        click(demo.disabledButton());

        assertEquals(before, demo.state().clicks());
        assertFalse(demo.disabledButton().isFocusable());
    }

    @Test
    void checkboxAndToggleSwitchFlipOnClick() {
        assertFalse(demo.checkbox().checked());
        assertFalse(demo.toggleSwitch().checked());
        assertTrue(demo.logCheckbox().checked(), "日志复选框默认勾选");

        click(demo.checkbox());
        click(demo.toggleSwitch());

        assertTrue(demo.checkbox().checked());
        assertTrue(demo.toggleSwitch().checked());
    }

    @Test
    void progressButtonsAdvanceAndClampStateAndBar() {
        click(demo.progressUpButton());
        click(demo.progressUpButton());

        assertEquals(0.2, demo.state().progress(), 1e-9);
        assertEquals(0.2, demo.progressBar().progress(), 1e-9);

        for (int i = 0; i < 12; i++) {
            click(demo.progressUpButton());
        }
        assertEquals(1.0, demo.state().progress(), 1e-9, "进度上限钳制到 1");
        assertEquals(1.0, demo.progressBar().progress(), 1e-9);

        click(demo.progressResetButton());
        assertEquals(0.0, demo.state().progress(), 1e-9);
        assertEquals(0.0, demo.progressBar().progress(), 1e-9);
    }

    @Test
    void itemSlotsBehaveAsSingleSelectionAndUpdateStatus() {
        assertFalse(demo.itemSlot1().isSelected());

        click(demo.itemSlot1());
        assertTrue(demo.itemSlot1().isSelected());
        assertTrue(demo.selectionStatus().text().contains("1"), demo.selectionStatus().text());

        click(demo.itemSlot2());
        assertTrue(demo.itemSlot2().isSelected());
        assertFalse(demo.itemSlot1().isSelected(), "选择互斥");
        assertTrue(demo.selectionStatus().text().contains("2"), demo.selectionStatus().text());

        click(demo.itemSlot2());
        assertFalse(demo.itemSlot2().isSelected());
        assertTrue(demo.selectionStatus().text().contains("无"), demo.selectionStatus().text());
    }

    @Test
    void rebuiltContentRestoresObservableState() {
        DemoState state = new DemoState();
        state.incrementClicks();
        state.incrementClicks();
        state.toggle();
        state.selectedSlot(2);

        // 主题切换会重建控件树：新树必须从 DemoState 恢复可观察状态。
        DemoContent rebuilt = DemoContent.build(state, PlainTextMetrics.INSTANCE);

        assertEquals("点击次数: 2", rebuilt.clickStatus().text());
        assertTrue(rebuilt.toggleStatus().text().contains("B"), rebuilt.toggleStatus().text());
        assertTrue(rebuilt.itemSlot2().isSelected(), "重建后应恢复槽位选择");
        assertFalse(rebuilt.itemSlot1().isSelected());
    }

    @Test
    void hoveringItemSlotShowsTooltip() {
        Rect slot = demo.itemSlot1().bounds();
        host.dispatch(new InputEvent.MouseMoved(slot.x() + 4, slot.y() + 4));

        assertTrue(host.activeTooltip().isPresent(), "悬停物品槽应显示工具提示");
        assertFalse(host.activeTooltip().orElseThrow().lines().isEmpty());

        host.dispatch(new InputEvent.MouseMoved(1, HEIGHT - 1));
        assertTrue(host.activeTooltip().isEmpty(), "移出后提示消失");
    }

    @Test
    void scrollPanelScrollsOnWheelWithinGallery() {
        Rect viewport = demo.scrollPanel().viewportBounds();
        assertEquals(0, demo.scrollPanel().scrollY());

        host.dispatch(new InputEvent.MouseScrolled(viewport.x() + 8, viewport.y() + 8, 0, -2));

        assertTrue(demo.scrollPanel().scrollY() > 0, "滚轮向下滚动");
        assertTrue(demo.scrollPanel().contentHeight() > viewport.height(), "内容高于视口");
    }

    @Test
    void clipShowcaseContainsOverflowingChild() {
        Rect show = demo.clipShowcase().bounds();
        assertTrue(descendantExceedsWidth(demo.clipShowcase(), show.width()), "裁剪演示必须包含一个溢出子节点");
    }

    @Test
    void demoRendersHeadlesslyWithClipAndText() {
        host.render(ctx);

        assertFalse(ctx.ops.isEmpty());
        assertTrue(ctx.ops.contains("pushClip(" + demo.clipShowcase().bounds().x() + ","
                + demo.clipShowcase().bounds().y() + ","
                + demo.clipShowcase().bounds().width() + ","
                + demo.clipShowcase().bounds().height() + ")"), "裁剪容器渲染时必须启用 scissor");
        assertTrue(ctx.ops.stream().anyMatch(op -> op.startsWith("text(")), "界面包含文本绘制");
    }

    private static boolean descendantExceedsWidth(Widget widget, int width) {
        for (Widget child : widget.children()) {
            if (child.bounds().width() > width || descendantExceedsWidth(child, width)) {
                return true;
            }
        }
        return false;
    }
}
