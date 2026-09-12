package com.gtsn.lib.ui.client;

import com.gtsn.lib.ui.theme.FontId;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 客户端字体工具（#23）：字体 id → {@code ResourceLocation}、资源可用性缓存，以及
 * “把字体套进 {@code Style.withFont} 再测量 / 绘制”的公共逻辑。
 *
 * <p>回退语义：{@code null} 或 {@link FontId#VANILLA} 表示原版默认字体（不套样式）；
 * 自定义字体资源缺失（未随包 / 资源包未加载）时同样回退原版，避免 {@code FontManager}
 * 落到缺字形集合。</p>
 *
 * <p>可用性缓存按 {@link ResourceManager} 实例失效：资源重载会替换管理器，缓存随之清空。</p>
 *
 * <p>客户端专用类：专职服务端不得加载（类加载纪律，见 ADR-0003/0004）。</p>
 */
public final class ClientFonts {

    private static final Logger LOGGER = LogUtils.getLogger();

    private static ResourceManager cachedManager;
    private static final Map<FontId, Boolean> AVAILABILITY = new HashMap<>();

    private ClientFonts() {
    }

    /** 字体定义 JSON 的资源位置：{@code <ns>:font/<path>.json}。 */
    public static ResourceLocation location(FontId fontId) {
        return new ResourceLocation(fontId.namespace(), "font/" + fontId.path() + ".json");
    }

    /** 字体定义资源是否可加载（同包默认字体恒为可用）。 */
    public static boolean ready(FontId fontId) {
        if (fontId == null || FontId.VANILLA.equals(fontId)) {
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        ResourceManager manager = minecraft != null ? minecraft.getResourceManager() : null;
        if (manager == null) {
            return false;
        }
        synchronized (AVAILABILITY) {
            if (manager != cachedManager) {
                AVAILABILITY.clear();
                cachedManager = manager;
            }
            return AVAILABILITY.computeIfAbsent(fontId, id -> {
                boolean present = manager.getResource(location(id)).isPresent();
                if (!present) {
                    LOGGER.warn("[GTSNLib] 字体资源缺失，回退原版字体: {}", id.location());
                }
                return present;
            });
        }
    }

    /** 实际可应用的字体：原版 / 缺失资源 → {@code null}（不套样式）。 */
    public static FontId effective(FontId requested) {
        if (requested == null || FontId.VANILLA.equals(requested) || !ready(requested)) {
            return null;
        }
        return requested;
    }

    /** 以 {@code font} 样式包装文本；字体不可应用时返回无样式组件。 */
    public static Component styled(String text, FontId requested) {
        Objects.requireNonNull(text, "text");
        MutableComponent component = Component.literal(text);
        FontId effective = effective(requested);
        return effective == null
                ? component
                : component.withStyle(style -> style.withFont(location(effective)));
    }

    /** 指定字体下的文本宽度（字体不可应用时等同 {@code font.width(text)}）。 */
    public static int width(Font font, String text, FontId requested) {
        Objects.requireNonNull(font, "font");
        FontId effective = effective(requested);
        return effective == null ? font.width(text) : font.width(styled(text, effective));
    }

    /** 清空可用性缓存（资源重载后兜底调用，正常路径由管理器实例变化自动失效）。 */
    public static void clearCache() {
        synchronized (AVAILABILITY) {
            AVAILABILITY.clear();
            cachedManager = null;
        }
    }
}
