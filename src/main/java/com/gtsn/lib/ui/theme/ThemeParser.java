package com.gtsn.lib.ui.theme;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.Reader;
import java.util.Map;
import java.util.Objects;

/**
 * 主题 JSON 解析器（无 MC 依赖，可无客户端单测）。
 *
 * <p>结构错误（JSON 语法、字段类型、颜色 / 标识格式）显式抛出 {@link ThemeParseException}；
 * 未知的颜色 / 纹理角色键与未知顶层小节被忽略，以保持前向兼容。</p>
 */
public final class ThemeParser {

    private ThemeParser() {
    }

    /** 从 JSON 字符串解析主题定义。 */
    public static ThemeDefinition parse(ThemeId id, String json) {
        Objects.requireNonNull(id, "id");
        if (json == null) {
            throw new ThemeParseException("theme json for " + id + " must not be null");
        }
        return parseElement(id, parseTree(() -> JsonParser.parseString(json), id));
    }

    /** 从 Reader 解析主题定义。 */
    public static ThemeDefinition parse(ThemeId id, Reader reader) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(reader, "reader");
        return parseElement(id, parseTree(() -> JsonParser.parseReader(reader), id));
    }

    private interface JsonSource {
        JsonElement read();
    }

    private static JsonElement parseTree(JsonSource source, ThemeId id) {
        try {
            return source.read();
        } catch (JsonParseException | IllegalStateException error) {
            throw new ThemeParseException("invalid theme json for " + id + ": " + error.getMessage(), error);
        }
    }

    private static ThemeDefinition parseElement(ThemeId id, JsonElement root) {
        if (root == null || !root.isJsonObject()) {
            throw new ThemeParseException("theme json for " + id + " must be a JSON object");
        }
        JsonObject object = root.getAsJsonObject();
        ThemeDefinition.Builder builder = ThemeDefinition.builder(id);

        if (object.has("name")) {
            builder.name(requireString(object, "name", "name"));
        }
        if (object.has("extends")) {
            builder.parent(parseThemeId(requireString(object, "extends", "extends"), "extends"));
        }
        parseColors(object, builder);
        parseText(object, builder);
        parseSpacing(object, builder);
        parseRounding(object, builder);
        parseTextures(object, builder);
        return builder.build();
    }

    private static void parseColors(JsonObject root, ThemeDefinition.Builder builder) {
        JsonObject colors = requireObject(root, "colors");
        if (colors == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : colors.entrySet()) {
            ThemeColorRole role = ThemeColorRole.fromKey(entry.getKey()).orElse(null);
            if (role == null) {
                continue; // 未知角色：忽略（前向兼容）
            }
            String raw = requireStringValue(entry.getValue(), "colors." + entry.getKey());
            builder.color(role, parseArgb(raw, "colors." + entry.getKey()));
        }
    }

    private static void parseText(JsonObject root, ThemeDefinition.Builder builder) {
        JsonObject text = requireObject(root, "text");
        if (text == null) {
            return;
        }
        if (text.has("shadow")) {
            JsonElement shadow = text.get("shadow");
            if (shadow == null || !shadow.isJsonPrimitive() || !shadow.getAsJsonPrimitive().isBoolean()) {
                throw new ThemeParseException("text.shadow must be a boolean");
            }
            builder.textShadow(shadow.getAsBoolean());
        }
        if (text.has("line_spacing")) {
            builder.textLineSpacing(requireNonNegativeInt(text, "line_spacing", "text.line_spacing"));
        }
        if (text.has("font")) {
            builder.textFont(parseFontId(requireString(text, "font", "text.font"), "text.font"));
        }
    }

    private static void parseSpacing(JsonObject root, ThemeDefinition.Builder builder) {
        JsonObject spacing = requireObject(root, "spacing");
        if (spacing == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : spacing.entrySet()) {
            Spacing step = switch (entry.getKey()) {
                case "xs" -> Spacing.XS;
                case "sm" -> Spacing.SM;
                case "md" -> Spacing.MD;
                case "lg" -> Spacing.LG;
                case "xl" -> Spacing.XL;
                default -> null;
            };
            if (step == null) {
                continue; // 未知间距阶：忽略
            }
            builder.spacing(step, requireNonNegativeIntValue(entry.getValue(), "spacing." + entry.getKey()));
        }
    }

    private static void parseRounding(JsonObject root, ThemeDefinition.Builder builder) {
        JsonObject rounding = requireObject(root, "rounding");
        if (rounding == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : rounding.entrySet()) {
            RoundingSize size = switch (entry.getKey()) {
                case "small" -> RoundingSize.SMALL;
                case "medium" -> RoundingSize.MEDIUM;
                case "large" -> RoundingSize.LARGE;
                default -> null;
            };
            if (size == null) {
                continue;
            }
            builder.rounding(size, requireNonNegativeIntValue(entry.getValue(), "rounding." + entry.getKey()));
        }
    }

    private static void parseTextures(JsonObject root, ThemeDefinition.Builder builder) {
        JsonObject textures = requireObject(root, "textures");
        if (textures == null) {
            return;
        }
        for (Map.Entry<String, JsonElement> entry : textures.entrySet()) {
            ThemeTextureRole role = ThemeTextureRole.fromKey(entry.getKey()).orElse(null);
            if (role == null) {
                continue;
            }
            String raw = requireStringValue(entry.getValue(), "textures." + entry.getKey());
            builder.texture(role, parseThemeId(raw, "textures." + entry.getKey()));
        }
    }

    private static ThemeId parseThemeId(String raw, String context) {
        try {
            return ThemeId.parse(raw);
        } catch (IllegalArgumentException error) {
            throw new ThemeParseException(context + " is not a valid id: " + raw, error);
        }
    }

    private static FontId parseFontId(String raw, String context) {
        try {
            return FontId.parse(raw);
        } catch (IllegalArgumentException error) {
            throw new ThemeParseException(context + " is not a valid font id: " + raw, error);
        }
    }

    /** 解析 {@code #RRGGBB} / {@code #AARRGGBB}（{@code #} 可省略；6 位视为不透明）。 */
    static int parseArgb(String raw, String context) {
        String text = raw.startsWith("#") ? raw.substring(1) : raw;
        if (text.length() != 6 && text.length() != 8) {
            throw new ThemeParseException(context + " must be #RRGGBB or #AARRGGBB: " + raw);
        }
        int value;
        try {
            value = (int) Long.parseLong(text, 16);
        } catch (NumberFormatException error) {
            throw new ThemeParseException(context + " must be hexadecimal: " + raw, error);
        }
        return text.length() == 6 ? 0xFF000000 | value : value;
    }

    private static JsonObject requireObject(JsonObject root, String key) {
        JsonElement element = root.get(key);
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (!element.isJsonObject()) {
            throw new ThemeParseException(key + " must be a JSON object");
        }
        return element.getAsJsonObject();
    }

    private static String requireString(JsonObject root, String key, String context) {
        return requireStringValue(root.get(key), context);
    }

    private static String requireStringValue(JsonElement element, String context) {
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new ThemeParseException(context + " must be a string");
        }
        return element.getAsString();
    }

    private static int requireNonNegativeInt(JsonObject root, String key, String context) {
        return requireNonNegativeIntValue(root.get(key), context);
    }

    private static int requireNonNegativeIntValue(JsonElement element, String context) {
        if (element == null || !element.isJsonPrimitive()) {
            throw new ThemeParseException(context + " must be an integer");
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (!primitive.isNumber()) {
            throw new ThemeParseException(context + " must be an integer");
        }
        double raw = primitive.getAsDouble();
        int value = (int) raw;
        if (raw != value) {
            throw new ThemeParseException(context + " must be an integer: " + primitive.getAsString());
        }
        if (value < 0) {
            throw new ThemeParseException(context + " must be non-negative: " + value);
        }
        return value;
    }
}
