package com.gtsn.lib.ui.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.gtsn.lib.ui.theme.FontId;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 客户端字体工具（#23）：字体 id → {@code ResourceLocation}、资源可用性缓存，以及
 * “把字体套进 {@code Style.withFont} 再测量 / 绘制”的公共逻辑。
 *
 * <p>回退语义：{@code null} 或 {@link FontId#VANILLA} 表示原版默认字体（不套样式）；
 * 自定义字体**定义或引用文件不可加载**（未随包 / 资源包未加载 / file 路径错误 / 非 sfnt）时
 * 同样回退原版——否则 {@code FontManager} 会落到 missing 字体集，渲染为豆腐块。</p>
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

    /** 字体定义 JSON 的资源位置：{@code <ns>:font/<path>.json}（仅用于存在性 / 内容校验）。 */
    public static ResourceLocation location(FontId fontId) {
        return new ResourceLocation(fontId.namespace(), "font/" + fontId.path() + ".json");
    }

    /** {@link net.minecraft.network.chat.Style#withFont} 使用的字体 id：{@code <ns>:<path>}（不是定义文件路径）。 */
    public static ResourceLocation styleFontId(FontId fontId) {
        return new ResourceLocation(fontId.namespace(), fontId.path());
    }

    /** 字体定义及其 {@code ttf} 引用文件是否可加载（原版默认字体恒为可用）。 */
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
            return AVAILABILITY.computeIfAbsent(fontId, id -> loadable(manager, id));
        }
    }

    /**
     * 定义与必备文件的存在性 / 基本格式校验：解析 {@code providers}，对每个 {@code ttf} provider
     * 按 {@code TrueTypeGlyphProviderDefinition.load} 的语义（{@code withPrefix("font/")}）解析文件，
     * 要求存在且以 sfnt magic 开头。任何一步失败 → 字体不可用（回退原版，绝不套 missing 字体集）。
     */
    private static boolean loadable(ResourceManager manager, FontId fontId) {
        Optional<Resource> definition = manager.getResource(location(fontId));
        if (definition.isEmpty()) {
            LOGGER.warn("[GTSNLib] 字体定义缺失，回退原版字体: {}", fontId.location());
            return false;
        }
        try (Reader reader = definition.orElseThrow().openAsReader()) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            JsonArray providers = root.getAsJsonArray("providers");
            if (providers == null || providers.isEmpty()) {
                LOGGER.warn("[GTSNLib] 字体定义无 providers，回退原版字体: {}", fontId.location());
                return false;
            }
            for (JsonElement element : providers) {
                JsonObject provider = element.getAsJsonObject();
                if (!provider.has("type") || !"ttf".equals(provider.get("type").getAsString())) {
                    continue;
                }
                ResourceLocation file = new ResourceLocation(provider.get("file").getAsString());
                ResourceLocation resolved = new ResourceLocation(file.getNamespace(), "font/" + file.getPath());
                Optional<Resource> resource = manager.getResource(resolved);
                if (resource.isEmpty() || !looksLikeSfnt(resource.orElseThrow())) {
                    LOGGER.warn("[GTSNLib] 字体 ttf 文件不可用，回退原版字体: {} → {}",
                            fontId.location(), resolved);
                    return false;
                }
            }
            return true;
        } catch (Exception error) {
            LOGGER.warn("[GTSNLib] 字体定义不可解析，回退原版字体: {}（{}）", fontId.location(), error.toString());
            return false;
        }
    }

    /** sfnt magic 校验：TrueType（0x00010000 / true）、CFF（OTTO）与集合（ttcf）。 */
    private static boolean looksLikeSfnt(Resource resource) {
        try (InputStream stream = resource.open()) {
            byte[] magic = stream.readNBytes(4);
            if (magic.length < 4) {
                return false;
            }
            String tag = new String(magic, StandardCharsets.ISO_8859_1);
            return tag.equals("\u0000\u0001\u0000\u0000")
                    || tag.equals("true") || tag.equals("OTTO") || tag.equals("ttcf");
        } catch (IOException error) {
            return false;
        }
    }

    /** 实际可应用的字体：原版 / 不可加载 → {@code null}（不套样式）。 */
    public static FontId effective(FontId requested) {
        if (requested == null || FontId.VANILLA.equals(requested) || !ready(requested)) {
            return null;
        }
        return requested;
    }

    /**
     * 供日志 / 探针使用的“实际生效字体 id”：{@link #effective(FontId)} 为空（原版请求或安全回退）
     * 时归一化为 {@link FontId#VANILLA}，保证日志里始终能看到一个确定的字体 id（#23 可观测性）。
     */
    public static FontId effectiveOrVanilla(FontId requested) {
        FontId applied = effective(requested);
        return applied == null ? FontId.VANILLA : applied;
    }

    /** 以 {@code font} 样式包装文本；字体不可应用时返回无样式组件。 */
    public static Component styled(String text, FontId requested) {
        Objects.requireNonNull(text, "text");
        MutableComponent component = Component.literal(text);
        FontId effective = effective(requested);
        return effective == null
                ? component
                : component.withStyle(style -> style.withFont(styleFontId(effective)));
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
