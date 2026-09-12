package com.gtsn.lib.gt.registration;

import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MaterialSpec} 声明构建与校验的行为验证：不依赖任何 Minecraft / Forge / GTCEu 类型。
 *
 * <p>覆盖外部可观察行为：必填 id/namespace 归一化、颜色范围、图标集、衍生件声明（含未知 flag 拒绝）、
 * 元素与组分、配方钩子存取。</p>
 */
class MaterialSpecTest {

    @Test
    void buildsWithAllDeclarations() {
        MaterialSpec spec = MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .color(0x8A2BE2)
                .iconSet(MaterialIcon.METALLIC)
                .parts(MaterialPart.INGOT, MaterialPart.PLATE, MaterialPart.DUST, MaterialPart.ROD)
                .element("Iron")
                .build();

        assertEquals("gtsnlib", spec.namespace());
        assertEquals("stellar_alloy", spec.id());
        assertEquals("gtsnlib:stellar_alloy", spec.key());
        assertEquals(0x8A2BE2, spec.color());
        assertEquals(MaterialIcon.METALLIC, spec.iconSet());
        assertEquals(Set.of(MaterialPart.INGOT, MaterialPart.PLATE, MaterialPart.DUST, MaterialPart.ROD),
                spec.parts());
        assertEquals(Optional.of("Iron"), spec.element());
        assertTrue(spec.components().isEmpty());
        assertTrue(spec.recipeHook().isEmpty());
    }

    @Test
    void normalizesNamespaceAndId() {
        MaterialSpec spec = MaterialSpec.builder(" GTSNLib ", " Stellar-Alloy ")
                .parts(MaterialPart.INGOT)
                .build();

        assertEquals("gtsnlib", spec.namespace());
        assertEquals("stellar_alloy", spec.id());
    }

    @Test
    void defaultsIconSetToMetallicAndColorToWhite() {
        MaterialSpec spec = MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .parts(MaterialPart.INGOT)
                .build();

        assertEquals(MaterialIcon.METALLIC, spec.iconSet());
        assertEquals(0xFFFFFF, spec.color());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> MaterialSpec.builder("gtsnlib", "  ")
                .parts(MaterialPart.INGOT)
                .build());
    }

    @Test
    void rejectsDullIdThatNormalizesToNothing() {
        assertThrows(IllegalArgumentException.class, () -> MaterialSpec.builder("gtsnlib", "!@#")
                .parts(MaterialPart.INGOT)
                .build());
    }

    @Test
    void rejectsBlankNamespace() {
        assertThrows(IllegalArgumentException.class, () -> MaterialSpec.builder(" ", "stellar_alloy")
                .parts(MaterialPart.INGOT)
                .build());
    }

    @Test
    void rejectsOutOfRangeColor() {
        assertThrows(IllegalArgumentException.class, () -> MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .color(0x1000000));
        assertThrows(IllegalArgumentException.class, () -> MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .color(-1));
    }

    @Test
    void rejectsNullIconSet() {
        assertThrows(NullPointerException.class, () -> MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .iconSet(null));
    }

    @Test
    void rejectsSpecWithoutDerivedParts() {
        assertThrows(IllegalArgumentException.class, () -> MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .build());
    }

    @Test
    void rejectsUnknownPartFlag() {
        assertThrows(IllegalArgumentException.class, () -> MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .part("unobtainium"));
    }

    @Test
    void acceptsPartFlagsByKey() {
        MaterialSpec spec = MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .parts("ingot", "plate", "dust", "rod")
                .build();

        assertEquals(Set.of(MaterialPart.INGOT, MaterialPart.PLATE, MaterialPart.DUST, MaterialPart.ROD),
                spec.parts());
    }

    @Test
    void deduplicatesRepeatedPartFlags() {
        MaterialSpec spec = MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .parts(MaterialPart.INGOT, MaterialPart.INGOT)
                .build();

        assertEquals(Set.of(MaterialPart.INGOT), spec.parts());
    }

    @Test
    void rejectsBlankElement() {
        assertThrows(IllegalArgumentException.class, () -> MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .element(" "));
    }

    @Test
    void storesComponents() {
        MaterialSpec spec = MaterialSpec.builder("gtsnlib", "steel")
                .parts(MaterialPart.INGOT)
                .component(new MaterialComponent("iron", 1))
                .component(new MaterialComponent("carbon", 1))
                .build();

        assertEquals(2, spec.components().size());
        assertEquals(new MaterialComponent("iron", 1), spec.components().get(0));
    }

    @Test
    void storesRecipeHook() {
        MaterialRecipeHook hook = registration -> {
        };

        MaterialSpec spec = MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .parts(MaterialPart.INGOT)
                .recipeHook(hook)
                .build();

        assertTrue(spec.recipeHook().isPresent());
        assertEquals(hook, spec.recipeHook().orElseThrow());
    }

    @Test
    void partsViewIsUnmodifiable() {
        MaterialSpec spec = MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .parts(MaterialPart.INGOT)
                .build();

        assertThrows(UnsupportedOperationException.class, () -> spec.parts().add(MaterialPart.PLATE));
    }

    @Test
    void materialComponentValidatesInput() {
        assertEquals(new MaterialComponent("iron", 2), new MaterialComponent("iron", 2));
        assertThrows(IllegalArgumentException.class, () -> new MaterialComponent(" ", 1));
        assertThrows(IllegalArgumentException.class, () -> new MaterialComponent("iron", 0));
    }

    @Test
    void materialPartRejectsUnknownKey() {
        assertEquals(MaterialPart.INGOT, MaterialPart.fromKey("Ingot"));
        assertThrows(IllegalArgumentException.class, () -> MaterialPart.fromKey("unobtainium"));
    }

    @Test
    void materialIconRejectsUnknownKey() {
        assertEquals(MaterialIcon.METALLIC, MaterialIcon.fromKey("metallic"));
        assertFalse(MaterialIcon.METALLIC.key().isBlank());
        assertThrows(IllegalArgumentException.class, () -> MaterialIcon.fromKey("unobtainium"));
    }
}
