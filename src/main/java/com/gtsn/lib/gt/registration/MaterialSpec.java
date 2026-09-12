package com.gtsn.lib.gt.registration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 声明式材料规格：用一处声明描述一种 GT 材料（id、颜色、图标集、衍生件、元素/组分、配方钩子），
 * 由适配层翻译为 GTCEu 注册调用。
 *
 * <p>纯数据模型，不依赖任何 Minecraft / Forge / GTCEu 类型，可在无游戏进程下完整单测。</p>
 *
 * <pre>
 *   MaterialSpec steel = MaterialSpec.builder("mymod", "stellar_alloy")
 *           .color(0x8A2BE2)
 *           .iconSet(MaterialIcon.METALLIC)
 *           .parts(MaterialPart.INGOT, MaterialPart.PLATE, MaterialPart.DUST, MaterialPart.ROD)
 *           .element("St")
 *           .recipeHook(registration -&gt; { ... })
 *           .build();
 * </pre>
 *
 * <p>id 与 namespace 在 {@link Builder#build()} 时归一化为 {@code [a-z0-9_]} 形式
 * （与 GTCEu {@code FormattingUtil} 的命名约定一致），因此声明可写 {@code "Stellar Alloy"}。</p>
 */
public final class MaterialSpec {

    private final String namespace;
    private final String id;
    private final int color;
    private final MaterialIcon iconSet;
    private final Set<MaterialPart> parts;
    private final Set<FluidState> fluidStates;
    private final String element;
    private final List<MaterialComponent> components;
    private final MaterialRecipeHook recipeHook;

    private MaterialSpec(Builder builder) {
        this.namespace = builder.namespace;
        this.id = builder.id;
        this.color = builder.color;
        this.iconSet = builder.iconSet;
        this.parts = Collections.unmodifiableSet(new LinkedHashSet<>(builder.parts));
        this.fluidStates = Collections.unmodifiableSet(new LinkedHashSet<>(builder.fluidStates));
        this.element = builder.element;
        this.components = List.copyOf(builder.components);
        this.recipeHook = builder.recipeHook;
    }

    /** 新建构建器；namespace 与 id 可包含大小写与分隔符，构建时归一化。 */
    public static Builder builder(String namespace, String id) {
        return new Builder(namespace, id);
    }

    /** 材料命名空间（已归一化），如 {@code gtsnlib}。 */
    public String namespace() {
        return namespace;
    }

    /** 材料 id 路径（已归一化），如 {@code stellar_alloy}。 */
    public String id() {
        return id;
    }

    /** 完整键 {@code namespace:id}，用于重复检测与日志。 */
    public String key() {
        return namespace + ":" + id;
    }

    /** RGB 颜色（0x000000..0xFFFFFF）。 */
    public int color() {
        return color;
    }

    /** 图标集。 */
    public MaterialIcon iconSet() {
        return iconSet;
    }

    /** 声明的衍生件类型（保持声明顺序，去重后只读）。 */
    public Set<MaterialPart> parts() {
        return parts;
    }

    /** 声明的流体形态（液体 / 气体 / 等离子体；保持声明顺序，去重后只读）。 */
    public Set<FluidState> fluidStates() {
        return fluidStates;
    }

    /** 是否声明了任何流体形态。 */
    public boolean hasFluidStates() {
        return !fluidStates.isEmpty();
    }

    /** 主元素符号（仅当声明为单质时存在）。 */
    public Optional<String> element() {
        return Optional.ofNullable(element);
    }

    /** 化学组分（仅当声明为化合物时非空）。 */
    public List<MaterialComponent> components() {
        return components;
    }

    /** 注册成功后的配方钩子（可选）。 */
    public Optional<MaterialRecipeHook> recipeHook() {
        return Optional.ofNullable(recipeHook);
    }

    /** {@link MaterialSpec} 的流式构建器；非法值在调用点或 {@link #build()} 时立即拒绝。 */
    public static final class Builder {

        private final String namespace;
        private final String id;
        private int color = 0xFFFFFF;
        private MaterialIcon iconSet = MaterialIcon.METALLIC;
        private final Set<MaterialPart> parts = new LinkedHashSet<>();
        private final Set<FluidState> fluidStates = new LinkedHashSet<>();
        private String element;
        private final List<MaterialComponent> components = new ArrayList<>();
        private MaterialRecipeHook recipeHook;

        private Builder(String namespace, String id) {
            this.namespace = normalize("namespace", namespace);
            this.id = normalize("id", id);
        }

        /** 设置 RGB 颜色。 */
        public Builder color(int color) {
            if (color < 0 || color > 0xFFFFFF) {
                throw new IllegalArgumentException("color must be within 0x000000..0xFFFFFF: " + color);
            }
            this.color = color;
            return this;
        }

        /** 设置图标集。 */
        public Builder iconSet(MaterialIcon iconSet) {
            this.iconSet = Objects.requireNonNull(iconSet, "iconSet");
            return this;
        }

        /** 追加一个衍生件。 */
        public Builder part(MaterialPart part) {
            parts.add(Objects.requireNonNull(part, "part"));
            return this;
        }

        /** 追加若干衍生件。 */
        public Builder parts(MaterialPart... parts) {
            Objects.requireNonNull(parts, "parts");
            for (MaterialPart part : parts) {
                part(part);
            }
            return this;
        }

        /**
         * 按声明键追加一个衍生件。
         *
         * @throws IllegalArgumentException 未知部件键
         */
        public Builder part(String key) {
            return part(MaterialPart.fromKey(key));
        }

        /**
         * 按声明键追加若干衍生件。
         *
         * @throws IllegalArgumentException 存在未知部件键
         */
        public Builder parts(String... keys) {
            Objects.requireNonNull(keys, "keys");
            for (String key : keys) {
                part(key);
            }
            return this;
        }

        /** 追加一个流体形态（材料流体：一键注册进 GT 流体体系）。 */
        public Builder fluid(FluidState state) {
            fluidStates.add(Objects.requireNonNull(state, "state"));
            return this;
        }

        /** 追加若干流体形态。 */
        public Builder fluids(FluidState... states) {
            Objects.requireNonNull(states, "states");
            for (FluidState state : states) {
                fluid(state);
            }
            return this;
        }

        /**
         * 按声明键追加一个流体形态。
         *
         * @throws IllegalArgumentException 未知物态键
         */
        public Builder fluid(String key) {
            return fluid(FluidState.fromKey(key));
        }

        /**
         * 按声明键追加若干流体形态。
         *
         * @throws IllegalArgumentException 存在未知物态键
         */
        public Builder fluids(String... keys) {
            Objects.requireNonNull(keys, "keys");
            for (String key : keys) {
                fluid(key);
            }
            return this;
        }

        /** 设置主元素符号（单质材料）。 */
        public Builder element(String element) {
            if (element != null && element.isBlank()) {
                throw new IllegalArgumentException("element must not be blank");
            }
            this.element = element == null ? null : element.trim();
            return this;
        }

        /** 追加一个化学组分（化合物材料）。 */
        public Builder component(MaterialComponent component) {
            components.add(Objects.requireNonNull(component, "component"));
            return this;
        }

        /** 追加若干化学组分。 */
        public Builder components(Collection<MaterialComponent> components) {
            Objects.requireNonNull(components, "components");
            for (MaterialComponent component : components) {
                component(component);
            }
            return this;
        }

        /** 设置注册成功后的配方钩子。 */
        public Builder recipeHook(MaterialRecipeHook recipeHook) {
            this.recipeHook = Objects.requireNonNull(recipeHook, "recipeHook");
            return this;
        }

        /**
         * 构建并校验规格。
         *
         * @throws IllegalArgumentException 未声明任何衍生件
         */
        public MaterialSpec build() {
            if (parts.isEmpty()) {
                throw new IllegalArgumentException("material must declare at least one part: " + id);
            }
            return new MaterialSpec(this);
        }

        /**
         * 归一化为 {@code [a-z0-9_]}：去空白、转小写、非字母数字折叠为单个下划线并去除首尾下划线。
         * 行为与适配层 {@code GtNames#normalizeName} 一致（此处不引用适配层以保持 registration → adapter
         * 单向无环）。
         */
        private static String normalize(String field, String raw) {
            Objects.requireNonNull(raw, field);
            String lower = raw.trim().toLowerCase(Locale.ROOT);
            StringBuilder out = new StringBuilder(lower.length());
            boolean lastWasUnderscore = false;
            for (int i = 0; i < lower.length(); i++) {
                char c = lower.charAt(i);
                boolean alphanumeric = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9');
                if (alphanumeric) {
                    out.append(c);
                    lastWasUnderscore = false;
                } else if (!lastWasUnderscore && out.length() > 0) {
                    out.append('_');
                    lastWasUnderscore = true;
                }
            }
            int end = out.length();
            while (end > 0 && out.charAt(end - 1) == '_') {
                end--;
            }
            String normalized = out.substring(0, end);
            if (normalized.isEmpty()) {
                throw new IllegalArgumentException(field + " must normalize to a non-empty id: " + raw);
            }
            return normalized;
        }
    }
}
