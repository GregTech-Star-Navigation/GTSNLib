package com.gtsn.lib.gt.adapter;

import com.gtsn.lib.gt.registration.MaterialPart;
import com.gtsn.lib.gt.registration.MaterialRegistration;
import com.gtsn.lib.gt.registration.MaterialSpec;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
            public MaterialRegistration registerMaterial(MaterialSpec spec) {
                throw new UnsupportedOperationException("not needed for lookup tests");
            }

            @Override
            public List<GtPartStatus> partStatus(String materialId, Set<MaterialPart> parts) {
                throw new UnsupportedOperationException("not needed for lookup tests");
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
}
