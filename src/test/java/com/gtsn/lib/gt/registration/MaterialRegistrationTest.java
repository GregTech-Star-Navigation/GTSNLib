package com.gtsn.lib.gt.registration;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MaterialRegistration} 结果视图的行为验证（#13：材料流体形态关联回材料）。
 */
class MaterialRegistrationTest {

    @Test
    void exposesFluidFormsByStateKey() {
        MaterialRegistration registration = new MaterialRegistration(
                "star_alloy", "gtsnlib", "gtsnlib:star_alloy",
                Map.of("ingot", "star_alloy_ingot"),
                java.util.List.of("forge:ingots/star_alloy"),
                Map.of("liquid", "gtsnlib:star_alloy", "gas", "gtsnlib:star_alloy_gas"));

        assertTrue(registration.hasFluid("liquid"));
        assertTrue(registration.hasFluid("gas"));
        assertFalse(registration.hasFluid("plasma"));
        assertEquals(Optional.of("gtsnlib:star_alloy_gas"), registration.fluid("gas"));
        assertEquals(Optional.empty(), registration.fluid("plasma"));
    }

    @Test
    void fluidMapDefaultsToEmptyForLegacyConstruction() {
        MaterialRegistration registration = new MaterialRegistration(
                "star_alloy", "gtsnlib", "gtsnlib:star_alloy",
                Map.of("ingot", "star_alloy_ingot"),
                java.util.List.of("forge:ingots/star_alloy"));

        assertTrue(registration.fluids().isEmpty());
        assertFalse(registration.hasFluid("liquid"));
    }
}
