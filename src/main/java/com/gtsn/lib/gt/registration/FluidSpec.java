package com.gtsn.lib.gt.registration;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * 声明式流体规格（#13）：用一处声明描述一种流体（id、物态、颜色、温度及其它 GT 属性），
 * 由适配层翻译为 GTCEu 流体注册调用。
 *
 * <p>流体有两种来源，二者互斥且必须明确声明其一：</p>
 * <ul>
 *   <li><b>独立（standalone）</b>：无材料的一次性流体，{@link Builder#standalone()}；</li>
 *   <li><b>材料关联（material-linked）</b>：声明所属材料，{@link Builder#material(String, String)}，
 *       用于把自定义流体关联回其材料。</li>
 * </ul>
 *
 * <p>纯数据模型，不依赖任何 Minecraft / Forge / GTCEu 类型，可在无游戏进程下完整单测。</p>
 *
 * <pre>
 *   FluidSpec liquidAir = FluidSpec.builder("mymod", "liquid_air")
 *           .standalone()
 *           .state(FluidState.GAS)
 *           .color(0x88CCFF)
 *           .temperature(300)
 *           .build();
 *
 *   FluidSpec alloyGas = FluidSpec.builder("mymod", "stellar_alloy_gas")
 *           .material("mymod", "stellar_alloy")
 *           .state(FluidState.GAS)
 *           .build();
 * </pre>
 *
 * <p>id 与 namespace 在 {@link Builder#build()} 时归一化为 {@code [a-z0-9_]} 形式
 * （与 GTCEu {@code FormattingUtil} 的命名约定一致），因此声明可写 {@code "Liquid Air"}。</p>
 */
public final class FluidSpec {

    /** 未显式声明颜色时的默认颜色（白）。 */
    public static final int DEFAULT_COLOR = 0xFFFFFF;
    /** 未显式声明温度时的默认温度（接近常温）。 */
    public static final int DEFAULT_TEMPERATURE = 300;
    /** 温度下界（开尔文）。 */
    public static final int MIN_TEMPERATURE = 1;
    /** 温度上界（开尔文）。 */
    public static final int MAX_TEMPERATURE = 100_000;
    /** 亮度上界（对齐 Minecraft 光照 0..15）。 */
    public static final int MAX_LUMINOSITY = 15;

    private final String namespace;
    private final String id;
    private final FluidState state;
    private final int color;
    private final int temperature;
    private final int density;
    private final int luminosity;
    private final int viscosity;
    private final int burnTime;
    private final boolean hasBlock;
    private final boolean hasBucket;
    private final MaterialLink material;
    private final FluidRegistrationHook registrationHook;

    private FluidSpec(Builder builder) {
        this.namespace = builder.namespace;
        this.id = builder.id;
        this.state = builder.state;
        this.color = builder.color;
        this.temperature = builder.temperature;
        this.density = builder.density;
        this.luminosity = builder.luminosity;
        this.viscosity = builder.viscosity;
        this.burnTime = builder.burnTime;
        this.hasBlock = builder.hasBlock;
        this.hasBucket = builder.hasBucket;
        this.material = builder.material;
        this.registrationHook = builder.registrationHook;
    }

    /** 新建构建器；namespace 与 id 可包含大小写与分隔符，构建时归一化。 */
    public static Builder builder(String namespace, String id) {
        return new Builder(namespace, id);
    }

    /** 流体命名空间（已归一化），如 {@code gtsnlib}。 */
    public String namespace() {
        return namespace;
    }

    /** 流体 id 路径（已归一化），如 {@code liquid_air}。 */
    public String id() {
        return id;
    }

    /** 完整键 {@code namespace:id}，用于重复检测与日志。 */
    public String key() {
        return namespace + ":" + id;
    }

    /** 物态。 */
    public FluidState state() {
        return state;
    }

    /** RGB 颜色（0x000000..0xFFFFFF）。 */
    public int color() {
        return color;
    }

    /** 温度（开尔文，{@link #MIN_TEMPERATURE}..{@link #MAX_TEMPERATURE}）。 */
    public int temperature() {
        return temperature;
    }

    /** 密度（GT 相对值，非负）。 */
    public int density() {
        return density;
    }

    /** 亮度（0..{@link #MAX_LUMINOSITY}）。 */
    public int luminosity() {
        return luminosity;
    }

    /** 粘度（GT 相对值，非负）。 */
    public int viscosity() {
        return viscosity;
    }

    /** 燃烧时间（tick，非负；0 表示不可燃）。 */
    public int burnTime() {
        return burnTime;
    }

    /** 是否生成流体方块。 */
    public boolean hasBlock() {
        return hasBlock;
    }

    /** 是否生成桶。 */
    public boolean hasBucket() {
        return hasBucket;
    }

    /** 是否为独立（无材料）流体。 */
    public boolean standalone() {
        return material == null;
    }

    /** 关联材料（仅材料关联流体存在）。 */
    public Optional<MaterialLink> material() {
        return Optional.ofNullable(material);
    }

    /** 注册成功后的回调（可选）。 */
    public Optional<FluidRegistrationHook> registrationHook() {
        return Optional.ofNullable(registrationHook);
    }

    /** 材料链接：材料命名空间 + 材料 id（均已归一化）。 */
    public record MaterialLink(String namespace, String id) {

        public MaterialLink {
            namespace = FluidSpec.normalize("namespace", namespace);
            id = FluidSpec.normalize("id", id);
        }

        /** 完整键 {@code namespace:id}。 */
        public String key() {
            return namespace + ":" + id;
        }
    }

    /** {@link FluidSpec} 的流式构建器；非法值在调用点或 {@link #build()} 时立即拒绝。 */
    public static final class Builder {

        private final String namespace;
        private final String id;
        private FluidState state;
        private int color = DEFAULT_COLOR;
        private int temperature = DEFAULT_TEMPERATURE;
        private int density;
        private int luminosity;
        private int viscosity;
        private int burnTime;
        private boolean hasBlock;
        private boolean hasBucket;
        private boolean originDeclared;
        private MaterialLink material;
        private FluidRegistrationHook registrationHook;

        private Builder(String namespace, String id) {
            this.namespace = FluidSpec.normalize("namespace", namespace);
            this.id = FluidSpec.normalize("id", id);
        }

        /** 声明为独立（无材料）流体。 */
        public Builder standalone() {
            if (material != null) {
                throw new IllegalStateException(
                        "fluid origin already declared as material: " + material.key());
            }
            originDeclared = true;
            return this;
        }

        /** 声明所属材料；与 {@link #standalone()} 互斥。 */
        public Builder material(String namespace, String id) {
            if (originDeclared && material == null) {
                throw new IllegalStateException("fluid origin already declared as standalone");
            }
            this.material = new MaterialLink(namespace, id);
            originDeclared = true;
            return this;
        }

        /** 设置物态。 */
        public Builder state(FluidState state) {
            this.state = Objects.requireNonNull(state, "state");
            return this;
        }

        /**
         * 按声明键设置物态。
         *
         * @throws IllegalArgumentException 未知物态键
         */
        public Builder state(String key) {
            return state(FluidState.fromKey(key));
        }

        /** 设置 RGB 颜色。 */
        public Builder color(int color) {
            if (color < 0 || color > 0xFFFFFF) {
                throw new IllegalArgumentException("color must be within 0x000000..0xFFFFFF: " + color);
            }
            this.color = color;
            return this;
        }

        /** 设置温度（开尔文）。 */
        public Builder temperature(int temperature) {
            if (temperature < MIN_TEMPERATURE || temperature > MAX_TEMPERATURE) {
                throw new IllegalArgumentException("temperature must be within "
                        + MIN_TEMPERATURE + ".." + MAX_TEMPERATURE + ": " + temperature);
            }
            this.temperature = temperature;
            return this;
        }

        /** 设置密度（GT 相对值）。 */
        public Builder density(int density) {
            if (density < 0) {
                throw new IllegalArgumentException("density must be non-negative: " + density);
            }
            this.density = density;
            return this;
        }

        /** 设置亮度。 */
        public Builder luminosity(int luminosity) {
            if (luminosity < 0 || luminosity > MAX_LUMINOSITY) {
                throw new IllegalArgumentException("luminosity must be within 0.." + MAX_LUMINOSITY
                        + ": " + luminosity);
            }
            this.luminosity = luminosity;
            return this;
        }

        /** 设置粘度（GT 相对值）。 */
        public Builder viscosity(int viscosity) {
            if (viscosity < 0) {
                throw new IllegalArgumentException("viscosity must be non-negative: " + viscosity);
            }
            this.viscosity = viscosity;
            return this;
        }

        /** 设置燃烧时间（tick）。 */
        public Builder burnTime(int burnTime) {
            if (burnTime < 0) {
                throw new IllegalArgumentException("burnTime must be non-negative: " + burnTime);
            }
            this.burnTime = burnTime;
            return this;
        }

        /** 是否生成流体方块。 */
        public Builder hasBlock(boolean hasBlock) {
            this.hasBlock = hasBlock;
            return this;
        }

        /** 是否生成桶。 */
        public Builder hasBucket(boolean hasBucket) {
            this.hasBucket = hasBucket;
            return this;
        }

        /** 设置注册成功后的回调。 */
        public Builder registrationHook(FluidRegistrationHook registrationHook) {
            this.registrationHook = Objects.requireNonNull(registrationHook, "registrationHook");
            return this;
        }

        /**
         * 构建并校验规格。
         *
         * @throws IllegalStateException 未声明物态，或未声明 / 同时声明了独立与材料来源
         */
        public FluidSpec build() {
            if (state == null) {
                throw new IllegalStateException("fluid must declare a state: " + id);
            }
            if (!originDeclared) {
                throw new IllegalStateException("fluid must declare standalone() or material(...): " + id);
            }
            return new FluidSpec(this);
        }
    }

    /**
     * 归一化为 {@code [a-z0-9_]}：去空白、转小写、非字母数字折叠为单个下划线并去除首尾下划线。
     * 与 {@link MaterialSpec} 的归一化行为一致。
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
