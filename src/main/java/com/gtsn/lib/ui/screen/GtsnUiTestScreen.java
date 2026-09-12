package com.gtsn.lib.ui.screen;

import com.gtsn.lib.ui.client.ThemeFontMetrics;
import com.gtsn.lib.ui.demo.DemoContent;
import com.gtsn.lib.ui.demo.DemoIcons;
import com.gtsn.lib.ui.demo.DemoState;
import com.gtsn.lib.ui.demo.ThemeControl;
import com.gtsn.lib.ui.render.ItemStackIcon;
import com.gtsn.lib.ui.render.SlotIcon;
import com.gtsn.lib.ui.theme.ThemeContext;
import com.gtsn.lib.ui.theme.ThemeId;
import com.gtsn.lib.ui.widget.TextMetrics;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.slf4j.Logger;

/**
 * 内置开发测试界面：装配 {@link DemoContent} 组件库（面板/文本/按钮/进度条/物品槽/滚动/选择/工具提示/主题切换），
 * 可在游戏内通过客户端命令 {@code /gtsnui} 打开（另见 {@code GTSNLIB_UI_AUTOTEST} 自动测试开关）。
 *
 * <p>主题（#18）：渲染由 {@link GtsnScreen} 携带当前主题；主题切换按钮经 {@link ThemeControl}
 * 改全局主题，本屏在 {@link #tick()} 检测到主题变化后用新主题重建控件树（间距等构造期度量随之更新，
 * {@link DemoState} 保留交互状态）。</p>
 *
 * <p>客户端专用类：专职服务端不得加载（类加载纪律，见 ADR-0003/0004）。</p>
 */
public final class GtsnUiTestScreen extends GtsnScreen {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 主题切换接缝：接全局主题上下文（{@code ThemeContext}）。 */
    private static final ThemeControl THEME_CONTROL = new ThemeControl() {
        @Override
        public String activeThemeName() {
            return ThemeContext.active().name();
        }

        @Override
        public void nextTheme() {
            ThemeContext.cycle();
        }
    };

    private final DemoState state;
    private DemoContent demo;
    private ThemeId renderedThemeId;
    private int renderedFrames;

    public GtsnUiTestScreen() {
        this(new DemoState());
    }

    public GtsnUiTestScreen(DemoState state) {
        this(buildDemo(state));
    }

    private GtsnUiTestScreen(DemoContent demo) {
        super(Component.literal("GTSN UI Components — dev test screen"), demo.root());
        this.state = demo.state();
        this.demo = demo;
        this.renderedThemeId = ThemeContext.activeId();
    }

    /** 客户端物品图标：真实物品堆叠渲染（GameTest 使用程序化占位）。 */
    private static DemoIcons clientIcons() {
        return new DemoIcons() {
            @Override
            public SlotIcon primary() {
                return ItemStackIcon.of(new ItemStack(Items.DIAMOND, 3));
            }

            @Override
            public SlotIcon secondary() {
                return ItemStackIcon.of(new ItemStack(Items.REDSTONE, 16));
            }
        };
    }

    private static DemoContent buildDemo(DemoState state) {
        return DemoContent.build(state, fontMetrics(), clientIcons(), THEME_CONTROL, ThemeContext.active());
    }

    public DemoContent demo() {
        return demo;
    }

    public int renderedFrames() {
        return renderedFrames;
    }

    @Override
    protected void init() {
        super.init();
        LOGGER.info("[GTSNLib] UI test screen init: {}x{} theme={}", width, height, ThemeContext.activeId());
    }

    @Override
    public void tick() {
        super.tick();
        ThemeId active = ThemeContext.activeId();
        if (!active.equals(renderedThemeId)) {
            renderedThemeId = active;
            demo = buildDemo(state);
            setRoot(demo.root());
            LOGGER.info("[GTSNLib] UI test screen rebuilt for theme {} ({})",
                    active, ThemeContext.active().name());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderedFrames++;
        if (renderedFrames == 1 || renderedFrames % 120 == 0) {
            LOGGER.info("[GTSNLib] UI test screen rendered: frame={} theme={} clicks={} typed={}",
                    renderedFrames, renderedThemeId, demo.state().clicks(), demo.state().typed());
        }
    }

    private static TextMetrics fontMetrics() {
        return new ThemeFontMetrics(Minecraft.getInstance().font);
    }
}
