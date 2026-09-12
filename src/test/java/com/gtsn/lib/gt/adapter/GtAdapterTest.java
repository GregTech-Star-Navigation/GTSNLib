package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.gt.registration.FluidRegistration;
import com.gtsn.lib.gt.registration.FluidSpec;
import com.gtsn.lib.gt.registration.FluidState;
import com.gtsn.lib.gt.registration.MaterialPart;
import com.gtsn.lib.gt.registration.MaterialRegistration;
import com.gtsn.lib.gt.registration.MaterialSpec;
import com.gtsn.lib.gt.registration.RegistrationKind;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link GtAdapter} 行为验证：注入假 {@link GtBackend}，不加载任何 GTCEu / MC 类型。
 *
 * <p>覆盖外部可观察行为：材料查询（按 id、大小写、资源位置）、tag prefix 查询（驼峰与下划线）、
 * 计数与查询结果汇总。</p>
 */
class GtAdapterTest {

    private static final GtMaterialRef IRON =
            new GtMaterialRef("iron", "gtceu", "iron", "gtceu:iron", "material.gtceu.iron", "Fe");

    private static final List<GtTagPrefixRef> PREFIXES = List.of(
            new GtTagPrefixRef("ingot", "ingot", "tagprefix.ingot"),
            new GtTagPrefixRef("tinyDust", "tiny_dust", "tagprefix.tiny_dust"));

    /** 假后端认为已注册的流体资源位置（state 由路径后缀推导）。 */
    private static final Set<String> PRESENT_FLUIDS = Set.of(
            "gtsnlib:liquid_air", "gtsnlib:star_alloy",
            "gtsnlib:star_alloy_gas", "gtsnlib:star_alloy_plasma");

    private static GtBackend backend(boolean available) {
        return new GtBackend() {
            @Override
            public boolean available() {
                return available;
            }

            @Override
            public Collection<GtMaterialRef> materials() {
                return List.of(IRON);
            }

            @Override
            public Collection<GtTagPrefixRef> tagPrefixes() {
                return PREFIXES;
            }

            @Override
            public GtRegistrateHandle registrate(String modId) {
                throw new UnsupportedOperationException("not needed for lookup tests");
            }

            @Override
            public GtRegistrateHandle createRegistrate(String modId) {
                throw new UnsupportedOperationException("not needed for lookup tests");
            }

            @Override
            public MaterialRegistration registerMaterial(MaterialSpec spec) {
                Map<String, String> fluids = new LinkedHashMap<>();
                for (FluidState state : spec.fluidStates()) {
                    fluids.put(state.key(), switch (state) {
                        case LIQUID -> spec.key();
                        case GAS -> spec.key() + "_gas";
                        case PLASMA -> spec.key() + "_plasma";
                    });
                }
                return new MaterialRegistration(spec.id(), spec.namespace(), spec.key(),
                        Map.of(), List.of(), fluids);
            }

            @Override
            public FluidRegistration registerFluid(FluidSpec spec) {
                return new FluidRegistration(spec.id(), spec.namespace(), spec.key(), spec.state(),
                        spec.material().map(FluidSpec.MaterialLink::key).orElse(""),
                        spec.namespace() + ":" + spec.id());
            }

            @Override
            public GtFluidStatus fluidStatus(String fluidId) {
                if (PRESENT_FLUIDS.contains(fluidId)) {
                    String state = fluidId.endsWith("_gas") ? "gas"
                            : fluidId.endsWith("_plasma") ? "plasma" : "liquid";
                    return GtFluidStatus.present(fluidId, available, fluidId, state, "");
                }
                return GtFluidStatus.missing(fluidId, available, "", "", "");
            }

            @Override
            public List<GtPartStatus> partStatus(String materialId, Set<MaterialPart> parts) {
                throw new UnsupportedOperationException("not needed for lookup tests");
            }

            @Override
            public GtContentStatus contentStatus(RegistrationKind kind, String id) {
                return GtContentStatus.missing(kind, id == null ? "" : id, available);
            }
        };
    }

    private static GtAdapter adapter() {
        return new GtAdapter(backend(true));
    }

    @Test
    void reportsAvailabilityFromBackend() {
        assertTrue(adapter().available());
        assertFalse(new GtAdapter(backend(false)).available());
    }

    @Test
    void findMaterialMatchesPlainId() {
        Optional<GtMaterialRef> found = adapter().findMaterial("iron");
        assertEquals(Optional.of(IRON), found);
    }

    @Test
    void findMaterialIsCaseInsensitive() {
        assertEquals(Optional.of(IRON), adapter().findMaterial("Iron"));
    }

    @Test
    void findMaterialMatchesFullyQualifiedResourceLocation() {
        assertEquals(Optional.of(IRON), adapter().findMaterial("gtceu:iron"));
    }

    @Test
    void findMaterialReturnsEmptyForUnknown() {
        assertEquals(Optional.empty(), adapter().findMaterial("gold"));
        assertEquals(Optional.empty(), adapter().findMaterial("  "));
        assertEquals(Optional.empty(), adapter().findMaterial(null));
    }

    @Test
    void findTagPrefixMatchesCamelCaseAndSnakeCase() {
        GtAdapter adapter = adapter();
        assertEquals("tinyDust", adapter.findTagPrefix("tinyDust").map(GtTagPrefixRef::name).orElseThrow());
        assertEquals("tinyDust", adapter.findTagPrefix("tiny_dust").map(GtTagPrefixRef::name).orElseThrow());
        assertEquals("ingot", adapter.findTagPrefix("Ingot").map(GtTagPrefixRef::name).orElseThrow());
        assertEquals(Optional.empty(), adapter.findTagPrefix("plate"));
    }

    @Test
    void countsTagPrefixesAndMaterials() {
        assertEquals(2, adapter().tagPrefixCount());
        assertEquals(1, adapter().materials().size());
    }

    @Test
    void querySummarizesPresentMaterial() {
        GtQueryResult result = adapter().query("iron");

        assertTrue(result.adapterAvailable());
        assertTrue(result.materialPresent());
        assertEquals("iron", result.query());
        assertEquals("gtceu:iron", result.materialResourceLocation());
        assertEquals("gtceu", result.materialModId());
        assertEquals("Fe", result.chemicalFormula());
        assertEquals(2, result.tagPrefixCount());
    }

    @Test
    void querySummarizesAbsentMaterial() {
        GtQueryResult result = adapter().query("gold");

        assertTrue(result.adapterAvailable());
        assertFalse(result.materialPresent());
        assertEquals("gold", result.query());
        assertEquals("", result.materialResourceLocation());
        assertEquals(2, result.tagPrefixCount());
    }

    // ---- #13: fluid / gas / plasma ----

    @Test
    void fluidStatusResolvesMaterialFluidForm() {
        GtAdapter adapter = adapter();
        adapter.registerMaterial(MaterialSpec.builder("gtsnlib", "star_alloy")
                .parts(MaterialPart.INGOT)
                .fluids(FluidState.LIQUID, FluidState.GAS, FluidState.PLASMA)
                .build());

        GtFluidStatus status = adapter.fluidStatus("gtsnlib:star_alloy_gas");

        assertTrue(status.present());
        assertEquals("gas", status.stateKey());
        assertEquals("gtsnlib:star_alloy", status.materialKey());
        assertEquals("gtsnlib:star_alloy_gas", status.fluidId());
    }

    @Test
    void materialFluidsListsDeclaredStates() {
        GtAdapter adapter = adapter();
        adapter.registerMaterial(MaterialSpec.builder("gtsnlib", "star_alloy")
                .parts(MaterialPart.INGOT)
                .fluids(FluidState.LIQUID, FluidState.GAS, FluidState.PLASMA)
                .build());

        List<GtFluidStatus> fluids = adapter.materialFluids("gtsnlib:star_alloy");

        assertEquals(3, fluids.size());
        assertTrue(fluids.stream().allMatch(GtFluidStatus::present));
        assertTrue(fluids.stream().allMatch(GtFluidStatus::materialLinked));
        assertEquals(Set.of("liquid", "gas", "plasma"),
                fluids.stream().map(GtFluidStatus::stateKey).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void fluidStatusResolvesStandaloneFluid() {
        GtAdapter adapter = adapter();
        adapter.registerFluid(FluidSpec.builder("gtsnlib", "liquid_air")
                .standalone()
                .state(FluidState.GAS)
                .build());

        assertTrue(adapter.isFluidRegistered("gtsnlib:liquid_air"));
        GtFluidStatus status = adapter.fluidStatus("gtsnlib:liquid_air");

        assertTrue(status.present());
        assertFalse(status.materialLinked());
        assertEquals("gas", status.stateKey());
        assertEquals("gtsnlib:liquid_air", status.fluidId());
    }

    @Test
    void rejectsDuplicateFluidRegistration() {
        GtAdapter adapter = adapter();
        FluidSpec spec = FluidSpec.builder("gtsnlib", "liquid_air")
                .standalone()
                .state(FluidState.GAS)
                .build();
        adapter.registerFluid(spec);

        assertThrows(IllegalArgumentException.class, () -> adapter.registerFluid(spec));
    }

    @Test
    void fluidStatusReportsAbsent() {
        GtFluidStatus status = adapter().fluidStatus("gtsnlib:nope");

        assertFalse(status.present());
        assertEquals("", status.fluidId());
        assertEquals("gtsnlib:nope", status.query());
    }
}
