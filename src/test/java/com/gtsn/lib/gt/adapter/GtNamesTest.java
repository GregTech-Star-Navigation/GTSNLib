package com.gtsn.lib.gt.adapter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * {@link GtNames} 纯映射逻辑验证：不依赖任何 Minecraft / Forge / GTCEu 类型，
 * 覆盖 id/name 归一化、资源位置构造与 tag prefix 映射。
 *
 * <p>预期值取自 GTCEu 7.5.3 的真实命名约定（{@code FormattingUtil.toLowerCaseUnderscore}
 * 与 {@code TagPrefix.getName()/getLowerCaseName()}），见实现类 javadoc 的出处引用。</p>
 */
class GtNamesTest {

    @Test
    void normalizeNameLowercasesAndTrims() {
        assertEquals("iron_ingot", GtNames.normalizeName("  Iron Ingot "));
    }

    @Test
    void normalizeNameConvertsSeparatorsToUnderscores() {
        assertEquals("red_alloy", GtNames.normalizeName("Red-Alloy"));
        assertEquals("a_b", GtNames.normalizeName("a__b"));
    }

    @Test
    void normalizeNameKeepsDigitsAndCamelCaseAsSingleToken() {
        assertEquals("ae2", GtNames.normalizeName("AE2"));
        assertEquals("dusttiny", GtNames.normalizeName("dustTiny"));
    }

    @Test
    void normalizeNameReturnsEmptyForAllInvalidInput() {
        assertEquals("", GtNames.normalizeName("!@#"));
    }

    @Test
    void toSnakeCaseMatchesGtPrefixLowerCasing() {
        // GTCEu: TagPrefix.getLowerCaseName() == FormattingUtil.toLowerCaseUnderscore(name)
        assertEquals("ingot", GtNames.toSnakeCase("ingot"));
        assertEquals("tiny_dust", GtNames.toSnakeCase("tinyDust"));
        assertEquals("small_dust", GtNames.toSnakeCase("smallDust"));
        assertEquals("raw_ore", GtNames.toSnakeCase("rawOre"));
        assertEquals("hot_ingot", GtNames.toSnakeCase("hotIngot"));
        assertEquals("wire_gt_single", GtNames.toSnakeCase("wireGtSingle"));
    }

    @Test
    void tagPrefixKeyNormalizesPrefixName() {
        assertEquals("tiny_dust", GtNames.tagPrefixKey("tinyDust"));
        assertEquals("tiny_dust", GtNames.tagPrefixKey("tiny_dust"));
        assertEquals("ingot", GtNames.tagPrefixKey("Ingot"));
    }

    @Test
    void resourceLocationJoinsValidNamespaceAndPath() {
        assertEquals("gtceu:iron", GtNames.resourceLocation("gtceu", "iron"));
    }

    @Test
    void resourceLocationRejectsInvalidNamespaceOrPath() {
        assertThrows(IllegalArgumentException.class, () -> GtNames.resourceLocation("GTCEu", "iron"));
        assertThrows(IllegalArgumentException.class, () -> GtNames.resourceLocation("gtceu", "Iron"));
        assertThrows(IllegalArgumentException.class, () -> GtNames.resourceLocation("gtceu", ""));
    }

    @Test
    void materialIdNormalizesBothParts() {
        assertEquals("gtceu:iron", GtNames.materialId("gtceu", "Iron"));
        assertEquals("gtceu:iron", GtNames.materialId("GTCEu", "Iron"));
    }

    @Test
    void derivedItemIdFollowsGtMaterialPrefixConvention() {
        assertEquals("iron_ingot", GtNames.derivedItemId("ingot", "Iron"));
        assertEquals("iron_tiny_dust", GtNames.derivedItemId("tinyDust", "Iron"));
    }

    @Test
    void tagPrefixKeyRejectsBlankInput() {
        assertThrows(IllegalArgumentException.class, () -> GtNames.tagPrefixKey("  "));
    }
}
