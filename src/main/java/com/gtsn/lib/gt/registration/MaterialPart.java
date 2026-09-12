package com.gtsn.lib.gt.registration;

import java.util.Locale;
import java.util.Objects;

/**
 * 声明式材料的衍生件类型：注册后由 GTCEu 自动生成对应的锭 / 板 / 粉 / 杆等物品或方块。
 *
 * <p>这是 GTSNLib 自有的稳定词汇（与 GTCEu 类型无关）。{@link com.gtsn.lib.gt.adapter} 内的适配层
 * 负责把每个部件映射到对应的 GTCEu {@code Material.Builder} 调用，从而保证本枚举不耦合上游。</p>
 */
public enum MaterialPart {

    /** 锭：触发 GTCEu 的锭属性（并附带粉属性）。 */
    INGOT("ingot"),
    /** 板：触发 GTCEu 的板生成标志。 */
    PLATE("plate"),
    /** 粉：触发 GTCEu 的粉属性。 */
    DUST("dust"),
    /** 杆：触发 GTCEu 的杆生成标志。 */
    ROD("rod"),
    /** 块：触发 GTCEu 的块生成标志。 */
    BLOCK("block"),
    /** 齿轮。 */
    GEAR("gear"),
    /** 螺丝。 */
    SCREW("screw"),
    /** 螺栓（与螺丝共用生成标志）。 */
    BOLT("bolt"),
    /** 环。 */
    RING("ring"),
    /** 箔。 */
    FOIL("foil");

    private final String key;

    MaterialPart(String key) {
        this.key = key;
    }

    /** 声明使用的稳定小写键，例如 {@code ingot}。 */
    public String key() {
        return key;
    }

    /**
     * 按声明键解析部件（大小写不敏感）。
     *
     * @throws IllegalArgumentException 键为空或未知
     */
    public static MaterialPart fromKey(String key) {
        Objects.requireNonNull(key, "key");
        String normalized = key.trim().toLowerCase(Locale.ROOT);
        for (MaterialPart part : values()) {
            if (part.key.equals(normalized)) {
                return part;
            }
        }
        throw new IllegalArgumentException("unknown material part: " + key);
    }
}
