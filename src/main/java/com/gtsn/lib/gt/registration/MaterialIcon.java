package com.gtsn.lib.gt.registration;

import java.util.Locale;
import java.util.Objects;

/**
 * 声明式材料的图标集（决定衍生件贴图风格与 GTCEu 材料图标集）。
 *
 * <p>GTSNLib 自有词汇；适配层负责映射到 GTCEu 的 {@code MaterialIconSet}。</p>
 */
public enum MaterialIcon {

    METALLIC("metallic"),
    DULL("dull"),
    MAGNETIC("magnetic"),
    SHINY("shiny"),
    BRIGHT("bright"),
    DIAMOND("diamond"),
    EMERALD("emerald"),
    GEM_HORIZONTAL("gem_horizontal"),
    GEM_VERTICAL("gem_vertical"),
    RUBY("ruby"),
    OPAL("opal"),
    GLASS("glass"),
    FINE("fine"),
    SAND("sand"),
    WOOD("wood"),
    ROUGH("rough"),
    FLINT("flint"),
    QUARTZ("quartz"),
    LAPIS("lapis"),
    RADIOACTIVE("radioactive");

    private final String key;

    MaterialIcon(String key) {
        this.key = key;
    }

    /** 声明使用的稳定小写键，例如 {@code metallic}。 */
    public String key() {
        return key;
    }

    /**
     * 按声明键解析图标集（大小写不敏感）。
     *
     * @throws IllegalArgumentException 键为空或未知
     */
    public static MaterialIcon fromKey(String key) {
        Objects.requireNonNull(key, "key");
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (MaterialIcon icon : values()) {
            if (icon.key.equals(normalized)) {
                return icon;
            }
        }
        throw new IllegalArgumentException("unknown material icon set: " + key);
    }
}
