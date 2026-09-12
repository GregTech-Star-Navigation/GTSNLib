package com.gtsn.lib.gt.registration;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FluidSpec} 声明构建与校验的行为验证：不依赖任何 Minecraft / Forge / GTCEu 类型（#13）。
 *
 * <p>覆盖外部可观察行为：必填 id/namespace 归一化、状态枚举、颜色范围、温度边界、独立与
 * 材料关联两种来源的互斥/必选、材料链接、注册钩子存取。</p>
 */
class FluidSpecTest {

    @Test
    void buildsStandaloneFluidWithAllDeclarations() {
        FluidRegistrationHook hook = registration -> {
        };
        FluidSpec spec = FluidSpec.builder("gtsnlib", "liquid_air")
                .standalone()
                .state(FluidState.LIQUID)
                .color(0x00FF00)
                .temperature(300)
                .density(1000)
                .luminosity(0)
                .viscosity(600)
                .burnTime(0)
                .hasBlock(true)
                .hasBucket(false)
                .registrationHook(hook)
                .build();

        assertEquals("gtsnlib", spec.namespace());
        assertEquals("liquid_air", spec.id());
        assertEquals("gtsnlib:liquid_air", spec.key());
        assertEquals(FluidState.LIQUID, spec.state());
        assertEquals(0x00FF00, spec.color());
        assertEquals(300, spec.temperature());
        assertEquals(1000, spec.density());
        assertEquals(0, spec.luminosity());
        assertEquals(600, spec.viscosity());
        assertEquals(0, spec.burnTime());
        assertTrue(spec.hasBlock());
        assertFalse(spec.hasBucket());
        assertTrue(spec.standalone());
        assertTrue(spec.material().isEmpty());
        assertTrue(spec.registrationHook().isPresent());
        assertEquals(hook, spec.registrationHook().orElseThrow());
    }

    @Test
    void buildsMaterialLinkedFluid() {
        FluidSpec spec = FluidSpec.builder("gtsnlib", "star_alloy_plasma")
                .material("gtsnlib", "star_alloy")
                .state(FluidState.PLASMA)
                .build();

        assertFalse(spec.standalone());
        FluidSpec.MaterialLink link = spec.material().orElseThrow();
        assertEquals("gtsnlib", link.namespace());
        assertEquals("star_alloy", link.id());
        assertEquals("gtsnlib:star_alloy", link.key());
    }

    @Test
    void normalizesNamespaceIdAndMaterialLink() {
        FluidSpec spec = FluidSpec.builder(" GTSNLib ", " Star-Alloy Plasma ")
                .material(" GTSNLib ", " Star-Alloy ")
                .state(FluidState.PLASMA)
                .build();

        assertEquals("gtsnlib", spec.namespace());
        assertEquals("star_alloy_plasma", spec.id());
        FluidSpec.MaterialLink link = spec.material().orElseThrow();
        assertEquals("gtsnlib", link.namespace());
        assertEquals("star_alloy", link.id());
    }

    @Test
    void defaultsStateRequiredAndColorAndTemperature() {
        FluidSpec spec = FluidSpec.builder("gtsnlib", "liquid_air")
                .standalone()
                .state(FluidState.GAS)
                .build();

        assertEquals(FluidState.GAS, spec.state());
        assertEquals(FluidSpec.DEFAULT_COLOR, spec.color());
        assertEquals(FluidSpec.DEFAULT_TEMPERATURE, spec.temperature());
        assertFalse(spec.hasBlock());
        assertFalse(spec.hasBucket());
    }

    @Test
    void rejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "  ")
                .standalone()
                .state(FluidState.LIQUID)
                .build());
    }

    @Test
    void rejectsDullIdThatNormalizesToNothing() {
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "!@#")
                .standalone()
                .state(FluidState.LIQUID)
                .build());
    }

    @Test
    void rejectsBlankNamespace() {
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder(" ", "liquid_air")
                .standalone()
                .state(FluidState.LIQUID)
                .build());
    }

    @Test
    void rejectsMissingState() {
        assertThrows(IllegalStateException.class, () -> FluidSpec.builder("gtsnlib", "liquid_air")
                .standalone()
                .build());
    }

    @Test
    void rejectsNullState() {
        assertThrows(NullPointerException.class, () -> FluidSpec.builder("gtsnlib", "liquid_air")
                .standalone()
                .state((FluidState) null));
    }

    @Test
    void rejectsUnknownStateKey() {
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "liquid_air")
                .standalone()
                .state("plasma_melt"));
    }

    @Test
    void parsesStateByKey() {
        FluidSpec spec = FluidSpec.builder("gtsnlib", "liquid_air")
                .standalone()
                .state("gas")
                .build();
        assertEquals(FluidState.GAS, spec.state());
    }

    @Test
    void rejectsOutOfRangeColor() {
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "liquid_air")
                .color(0x1000000));
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "liquid_air")
                .color(-1));
    }

    @Test
    void rejectsTemperatureOutOfBounds() {
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "liquid_air")
                .temperature(FluidSpec.MIN_TEMPERATURE - 1));
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "liquid_air")
                .temperature(FluidSpec.MAX_TEMPERATURE + 1));
    }

    @Test
    void acceptsTemperatureAtBounds() {
        assertEquals(FluidSpec.MIN_TEMPERATURE, FluidSpec.builder("gtsnlib", "cold")
                .standalone().state(FluidState.LIQUID)
                .temperature(FluidSpec.MIN_TEMPERATURE).build().temperature());
        assertEquals(FluidSpec.MAX_TEMPERATURE, FluidSpec.builder("gtsnlib", "hot")
                .standalone().state(FluidState.PLASMA)
                .temperature(FluidSpec.MAX_TEMPERATURE).build().temperature());
    }

    @Test
    void rejectsNegativeDensityViscosityAndBurnTime() {
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "a")
                .density(-1));
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "a")
                .viscosity(-1));
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "a")
                .burnTime(-1));
    }

    @Test
    void rejectsLuminosityOutOfRange() {
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "a")
                .luminosity(-1));
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "a")
                .luminosity(16));
    }

    @Test
    void requiresExactlyOneOrigin() {
        assertThrows(IllegalStateException.class, () -> FluidSpec.builder("gtsnlib", "liquid_air")
                .state(FluidState.LIQUID)
                .build());
    }

    @Test
    void standaloneAndMaterialAreMutuallyExclusive() {
        assertThrows(IllegalStateException.class, () -> FluidSpec.builder("gtsnlib", "liquid_air")
                .standalone()
                .material("gtsnlib", "star_alloy"));
        assertThrows(IllegalStateException.class, () -> FluidSpec.builder("gtsnlib", "liquid_air")
                .material("gtsnlib", "star_alloy")
                .standalone());
    }

    @Test
    void rejectsBlankMaterialLink() {
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "liquid_air")
                .material(" ", "star_alloy"));
        assertThrows(IllegalArgumentException.class, () -> FluidSpec.builder("gtsnlib", "liquid_air")
                .material("gtsnlib", " "));
    }

    @Test
    void materialLinkIsAbsentForStandalone() {
        FluidSpec spec = FluidSpec.builder("gtsnlib", "liquid_air")
                .standalone()
                .state(FluidState.LIQUID)
                .build();
        assertEquals(Optional.empty(), spec.material());
    }

    @Test
    void stateIsRequiredWhenMaterialLinked() {
        assertThrows(IllegalStateException.class, () -> FluidSpec.builder("gtsnlib", "liquid_air")
                .material("gtsnlib", "star_alloy")
                .build());
    }

    @Test
    void fluidStateRejectsUnknownKey() {
        assertEquals(FluidState.PLASMA, FluidState.fromKey("Plasma"));
        assertThrows(IllegalArgumentException.class, () -> FluidState.fromKey("solid"));
        assertFalse(FluidState.LIQUID.key().isBlank());
    }
}
