package com.gtsn.lib.compat.mekanism;

import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * 声明式化学物质规格（#14）：用一处声明描述一种 Mekanism 化学物质（id、种类、颜色、隐藏标记、
 * 浆液矿词关联），由隔离翻译层 {@code com.gtsn.lib.integration.mekanism} 翻译为 Mekanism 注册调用。
 *
 * <p>纯数据模型，不依赖任何 Minecraft / Forge / Mekanism 类型，可在无游戏进程下完整单测。</p>
 *
 * <pre>
 *   ChemicalSpec gas = ChemicalSpec.builder("mymod", "stellar_gas")
 *           .kind(ChemicalKind.GAS)
 *           .tint(0x88CCFF)
 *           .build();
 *
 *   ChemicalSpec slurry = ChemicalSpec.builder("mymod", "stellar_alloy")
 *           .kind(ChemicalKind.SLURRY)
 *           .ore("mymod", "stellar_alloy")
 *           .build();
 * </pre>
 *
 * <p>id 与 namespace 在构建时归一化为 {@code [a-z0-9_]} 形式（与 {@link com.gtsn.lib.gt.registration.MaterialSpec}
 * 的命名约定一致），因此声明可写 {@code "Stellar Gas"}。</p>
 */
public final class ChemicalSpec {

    /** 未显式声明颜色时的默认颜色（白）。 */
    public static final int DEFAULT_TINT = 0xFFFFFF;

    private final String namespace;
    private final String id;
    private final ChemicalKind kind;
    private final int tint;
    private final boolean hidden;
    private final OreTag oreTag;
    private final ChemicalRegistrationHook registrationHook;

    private ChemicalSpec(Builder builder) {
        this.namespace = builder.namespace;
        this.id = builder.id;
        this.kind = builder.kind;
        this.tint = builder.tint;
        this.hidden = builder.hidden;
        this.oreTag = builder.oreTag;
        this.registrationHook = builder.registrationHook;
    }

    /** 新建构建器；namespace 与 id 可包含大小写与分隔符，构建时归一化。 */
    public static Builder builder(String namespace, String id) {
        return new Builder(namespace, id);
    }

    /** 命名空间（已归一化），如 {@code gtsnlib}。 */
    public String namespace() {
        return namespace;
    }

    /** 声明 id 路径（已归一化），如 {@code test_chemical}。 */
    public String id() {
        return id;
    }

    /** 完整键 {@code namespace:id}，用于重复检测与日志。 */
    public String key() {
        return namespace + ":" + id;
    }

    /** 化学种类。 */
    public ChemicalKind kind() {
        return kind;
    }

    /** RGB 颜色（0x000000..0xFFFFFF）。 */
    public int tint() {
        return tint;
    }

    /** 是否在 JEI 等界面隐藏。 */
    public boolean hidden() {
        return hidden;
    }

    /** 浆液矿词关联（仅浆液存在）。 */
    public Optional<OreTag> oreTag() {
        return Optional.ofNullable(oreTag);
    }

    /** 注册成功后的回调（可选）。 */
    public Optional<ChemicalRegistrationHook> registrationHook() {
        return Optional.ofNullable(registrationHook);
    }

    /** 矿词链接：矿词命名空间 + 矿词 id（均已归一化）。 */
    public record OreTag(String namespace, String id) {

        public OreTag {
            namespace = ChemicalSpec.normalize("namespace", namespace);
            id = ChemicalSpec.normalize("id", id);
        }

        /** 完整键 {@code namespace:id}。 */
        public String key() {
            return namespace + ":" + id;
        }
    }

    /** {@link ChemicalSpec} 的流式构建器；非法值在调用点或 {@link #build()} 时立即拒绝。 */
    public static final class Builder {

        private final String namespace;
        private final String id;
        private ChemicalKind kind;
        private int tint = DEFAULT_TINT;
        private boolean hidden;
        private OreTag oreTag;
        private ChemicalRegistrationHook registrationHook;

        private Builder(String namespace, String id) {
            this.namespace = ChemicalSpec.normalize("namespace", namespace);
            this.id = ChemicalSpec.normalize("id", id);
        }

        /** 设置化学种类。 */
        public Builder kind(ChemicalKind kind) {
            this.kind = Objects.requireNonNull(kind, "kind");
            return this;
        }

        /**
         * 按声明键设置化学种类。
         *
         * @throws IllegalArgumentException 未知种类键
         */
        public Builder kind(String key) {
            return kind(ChemicalKind.fromKey(key));
        }

        /** 设置 RGB 颜色。 */
        public Builder tint(int tint) {
            if (tint < 0 || tint > 0xFFFFFF) {
                throw new IllegalArgumentException("tint must be within 0x000000..0xFFFFFF: " + tint);
            }
            this.tint = tint;
            return this;
        }

        /** 是否在 JEI 等界面隐藏。 */
        public Builder hidden(boolean hidden) {
            this.hidden = hidden;
            return this;
        }

        /** 关联矿词（仅浆液合法）；矿词命名空间与 id 归一化。 */
        public Builder ore(String namespace, String id) {
            this.oreTag = new OreTag(namespace, id);
            return this;
        }

        /** 设置注册成功后的回调。 */
        public Builder registrationHook(ChemicalRegistrationHook registrationHook) {
            this.registrationHook = Objects.requireNonNull(registrationHook, "registrationHook");
            return this;
        }

        /**
         * 构建并校验规格。
         *
         * @throws IllegalStateException 未声明化学种类，或在非浆液上声明了矿词关联
         */
        public ChemicalSpec build() {
            if (kind == null) {
                throw new IllegalStateException("chemical must declare a kind: " + id);
            }
            if (oreTag != null && kind != ChemicalKind.SLURRY) {
                throw new IllegalStateException(
                        "ore tag is only valid for slurries: " + id + " (" + kind.key() + ")");
            }
            return new ChemicalSpec(this);
        }
    }

    /**
     * 归一化为 {@code [a-z0-9_]}：去空白、转小写、非字母数字折叠为单个下划线并去除首尾下划线。
     * 与 {@code MaterialSpec}/{@code FluidSpec} 的归一化行为一致。
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
