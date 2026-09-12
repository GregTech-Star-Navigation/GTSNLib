package com.gtsn.lib.ui.theme;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * 主题定义（未解析继承链的原始数据）：来源于 JSON 或 {@link Builder} 的编程式构造。
 *
 * <p>定义只记录显式声明的值；缺失角色 / 度量的回退发生在 {@link Theme} 沿继承链取值时。</p>
 */
public final class ThemeDefinition {

    private final ThemeId id;
    private final String name;
    private final ThemeId parentId;
    private final Map<ThemeColorRole, Integer> colors;
    private final Boolean textShadow;
    private final Integer textLineSpacing;
    private final Map<Spacing, Integer> spacing;
    private final Map<RoundingSize, Integer> rounding;
    private final Map<ThemeTextureRole, ThemeId> textures;

    private ThemeDefinition(Builder builder) {
        this.id = builder.id;
        this.name = builder.name != null ? builder.name : builder.id.path();
        this.parentId = builder.parentId;
        this.colors = Collections.unmodifiableMap(new EnumMap<>(builder.colors));
        this.textShadow = builder.textShadow;
        this.textLineSpacing = builder.textLineSpacing;
        this.spacing = Collections.unmodifiableMap(new EnumMap<>(builder.spacing));
        this.rounding = Collections.unmodifiableMap(new EnumMap<>(builder.rounding));
        this.textures = Collections.unmodifiableMap(new EnumMap<>(builder.textures));
    }

    public static Builder builder(ThemeId id) {
        return new Builder(id);
    }

    /** 无损复制一个定义（用于测试 / 编程式覆盖）。 */
    public Builder toBuilder() {
        Builder builder = new Builder(id)
                .name(name)
                .parent(parentId)
                .textShadow(textShadow)
                .textLineSpacing(textLineSpacing);
        colors.forEach(builder::color);
        spacing.forEach(builder::spacing);
        rounding.forEach(builder::rounding);
        textures.forEach(builder::texture);
        return builder;
    }

    public ThemeId id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Optional<ThemeId> parentId() {
        return Optional.ofNullable(parentId);
    }

    public Map<ThemeColorRole, Integer> colors() {
        return colors;
    }

    public OptionalInt color(ThemeColorRole role) {
        Integer value = colors.get(Objects.requireNonNull(role, "role"));
        return value != null ? OptionalInt.of(value) : OptionalInt.empty();
    }

    public Optional<Boolean> textShadow() {
        return Optional.ofNullable(textShadow);
    }

    public OptionalInt textLineSpacing() {
        return textLineSpacing != null ? OptionalInt.of(textLineSpacing) : OptionalInt.empty();
    }

    public OptionalInt spacing(Spacing step) {
        Integer value = spacing.get(Objects.requireNonNull(step, "step"));
        return value != null ? OptionalInt.of(value) : OptionalInt.empty();
    }

    public OptionalInt rounding(RoundingSize size) {
        Integer value = rounding.get(Objects.requireNonNull(size, "size"));
        return value != null ? OptionalInt.of(value) : OptionalInt.empty();
    }

    public Map<ThemeTextureRole, ThemeId> textures() {
        return textures;
    }

    public Optional<ThemeId> texture(ThemeTextureRole role) {
        return Optional.ofNullable(textures.get(Objects.requireNonNull(role, "role")));
    }

    /** 增量构造器；未声明的字段保持“未设”语义。 */
    public static final class Builder {

        private final ThemeId id;
        private String name;
        private ThemeId parentId;
        private final Map<ThemeColorRole, Integer> colors = new EnumMap<>(ThemeColorRole.class);
        private Boolean textShadow;
        private Integer textLineSpacing;
        private final Map<Spacing, Integer> spacing = new EnumMap<>(Spacing.class);
        private final Map<RoundingSize, Integer> rounding = new EnumMap<>(RoundingSize.class);
        private final Map<ThemeTextureRole, ThemeId> textures = new EnumMap<>(ThemeTextureRole.class);

        private Builder(ThemeId id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder name(String name) {
            this.name = Objects.requireNonNull(name, "name");
            return this;
        }

        public Builder parent(ThemeId parentId) {
            this.parentId = parentId;
            return this;
        }

        public Builder color(ThemeColorRole role, int argb) {
            colors.put(Objects.requireNonNull(role, "role"), argb);
            return this;
        }

        public Builder textShadow(Boolean shadow) {
            this.textShadow = shadow;
            return this;
        }

        public Builder textLineSpacing(Integer lineSpacing) {
            if (lineSpacing != null && lineSpacing < 0) {
                throw new IllegalArgumentException("line spacing must be non-negative: " + lineSpacing);
            }
            this.textLineSpacing = lineSpacing;
            return this;
        }

        public Builder spacing(Spacing step, int pixels) {
            if (pixels < 0) {
                throw new IllegalArgumentException("spacing must be non-negative: " + pixels);
            }
            spacing.put(Objects.requireNonNull(step, "step"), pixels);
            return this;
        }

        public Builder rounding(RoundingSize size, int pixels) {
            if (pixels < 0) {
                throw new IllegalArgumentException("rounding must be non-negative: " + pixels);
            }
            rounding.put(Objects.requireNonNull(size, "size"), pixels);
            return this;
        }

        public Builder texture(ThemeTextureRole role, ThemeId texture) {
            textures.put(Objects.requireNonNull(role, "role"), Objects.requireNonNull(texture, "texture"));
            return this;
        }

        public ThemeDefinition build() {
            return new ThemeDefinition(this);
        }
    }
}
