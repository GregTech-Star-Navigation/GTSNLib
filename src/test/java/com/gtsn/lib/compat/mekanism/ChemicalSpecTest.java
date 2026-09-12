package com.gtsn.lib.compat.mekanism;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ChemicalSpec} 声明构建与校验的行为验证：不依赖任何 Minecraft / Forge / Mekanism 类型（#14）。
 *
 * <p>覆盖外部可观察行为：id/namespace 归一化、化学种类必填与解析、颜色范围、隐藏标记、
 * 浆液矿词关联（仅浆液合法）、注册钩子存取。</p>
 */
class ChemicalSpecTest {

    @Test
    void buildsGasWithAllDeclarations() {
        ChemicalRegistrationHook hook = registration -> {
        };
        ChemicalSpec spec = ChemicalSpec.builder("gtsnlib", "test_chemical")
                .kind(ChemicalKind.GAS)
                .tint(0x88CCFF)
                .hidden(true)
                .registrationHook(hook)
                .build();

        assertEquals("gtsnlib", spec.namespace());
        assertEquals("test_chemical", spec.id());
        assertEquals("gtsnlib:test_chemical", spec.key());
        assertEquals(ChemicalKind.GAS, spec.kind());
        assertEquals(0x88CCFF, spec.tint());
        assertTrue(spec.hidden());
        assertTrue(spec.oreTag().isEmpty());
        assertTrue(spec.registrationHook().isPresent());
        assertEquals(hook, spec.registrationHook().orElseThrow());
    }

    @Test
    void defaultsTintAndHidden() {
        ChemicalSpec spec = ChemicalSpec.builder("gtsnlib", "test_chemical")
                .kind(ChemicalKind.INFUSE_TYPE)
                .build();

        assertEquals(ChemicalSpec.DEFAULT_TINT, spec.tint());
        assertFalse(spec.hidden());
    }

    @Test
    void buildsSlurryWithOreTag() {
        ChemicalSpec spec = ChemicalSpec.builder("gtsnlib", "star_alloy")
                .kind(ChemicalKind.SLURRY)
                .ore("gtsnlib", "star_alloy")
                .build();

        assertEquals(ChemicalKind.SLURRY, spec.kind());
        ChemicalSpec.OreTag ore = spec.oreTag().orElseThrow();
        assertEquals("gtsnlib", ore.namespace());
        assertEquals("star_alloy", ore.id());
        assertEquals("gtsnlib:star_alloy", ore.key());
    }

    @Test
    void normalizesNamespaceIdAndOreLink() {
        ChemicalSpec spec = ChemicalSpec.builder(" GTSNLib ", " Test-Chemical ")
                .kind(ChemicalKind.SLURRY)
                .ore(" GTSNLib ", " Star-Alloy ")
                .build();

        assertEquals("gtsnlib", spec.namespace());
        assertEquals("test_chemical", spec.id());
        ChemicalSpec.OreTag ore = spec.oreTag().orElseThrow();
        assertEquals("gtsnlib", ore.namespace());
        assertEquals("star_alloy", ore.id());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> ChemicalSpec.builder("gtsnlib", "  ")
                .kind(ChemicalKind.GAS)
                .build());
    }

    @Test
    void rejectsDullIdThatNormalizesToNothing() {
        assertThrows(IllegalArgumentException.class, () -> ChemicalSpec.builder("gtsnlib", "!@#")
                .kind(ChemicalKind.GAS)
                .build());
    }

    @Test
    void rejectsBlankNamespace() {
        assertThrows(IllegalArgumentException.class, () -> ChemicalSpec.builder(" ", "test_chemical")
                .kind(ChemicalKind.GAS)
                .build());
    }

    @Test
    void rejectsMissingKind() {
        assertThrows(IllegalStateException.class, () -> ChemicalSpec.builder("gtsnlib", "test_chemical")
                .build());
    }

    @Test
    void rejectsNullKind() {
        assertThrows(NullPointerException.class, () -> ChemicalSpec.builder("gtsnlib", "test_chemical")
                .kind((ChemicalKind) null));
    }

    @Test
    void rejectsUnknownKindKey() {
        assertThrows(IllegalArgumentException.class, () -> ChemicalSpec.builder("gtsnlib", "test_chemical")
                .kind("plasma"));
    }

    @Test
    void parsesKindByKey() {
        ChemicalSpec spec = ChemicalSpec.builder("gtsnlib", "test_slurry")
                .kind("slurry")
                .build();
        assertEquals(ChemicalKind.SLURRY, spec.kind());
    }

    @Test
    void rejectsOutOfRangeTint() {
        assertThrows(IllegalArgumentException.class, () -> ChemicalSpec.builder("gtsnlib", "test_chemical")
                .tint(0x1000000));
        assertThrows(IllegalArgumentException.class, () -> ChemicalSpec.builder("gtsnlib", "test_chemical")
                .tint(-1));
    }

    @Test
    void rejectsOreTagOnNonSlurry() {
        assertThrows(IllegalStateException.class, () -> ChemicalSpec.builder("gtsnlib", "test_chemical")
                .kind(ChemicalKind.GAS)
                .ore("gtsnlib", "star_alloy")
                .build());
    }

    @Test
    void rejectsBlankOreLink() {
        assertThrows(IllegalArgumentException.class, () -> ChemicalSpec.builder("gtsnlib", "star_alloy")
                .kind(ChemicalKind.SLURRY)
                .ore(" ", "star_alloy"));
        assertThrows(IllegalArgumentException.class, () -> ChemicalSpec.builder("gtsnlib", "star_alloy")
                .kind(ChemicalKind.SLURRY)
                .ore("gtsnlib", " "));
    }

    @Test
    void chemicalKindExposesStableKeyAndRegistryId() {
        assertEquals("gas", ChemicalKind.GAS.key());
        assertEquals("mekanism:gas", ChemicalKind.GAS.registryId());
        assertEquals("slurry", ChemicalKind.SLURRY.key());
        assertEquals("mekanism:slurry", ChemicalKind.SLURRY.registryId());
        assertEquals(ChemicalKind.INFUSE_TYPE, ChemicalKind.fromKey("Infuse_Type"));
        assertThrows(IllegalArgumentException.class, () -> ChemicalKind.fromKey("solid"));
    }
}
