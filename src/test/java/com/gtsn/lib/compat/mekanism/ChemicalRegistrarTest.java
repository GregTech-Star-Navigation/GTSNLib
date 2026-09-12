package com.gtsn.lib.compat.mekanism;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ChemicalRegistrar} 行为验证：注入假 sink，不加载 Mekanism（#14）。
 *
 * <p>覆盖外部可观察行为：经 sink 注册并返回结果、重复声明拒绝、注册钩子在 sink 成功后回调、
 * 失败时不回调、已注册查询、浆液双资源位置透传。</p>
 */
class ChemicalRegistrarTest {

    private static ChemicalSpec gas(String id) {
        return ChemicalSpec.builder("gtsnlib", id)
                .kind(ChemicalKind.GAS)
                .tint(0x88CCFF)
                .build();
    }

    private static ChemicalRegistration registration(ChemicalSpec spec) {
        return new ChemicalRegistration(
                spec.id(),
                spec.namespace(),
                spec.key(),
                spec.kind(),
                spec.kind().registryId(),
                spec.tint(),
                spec.hidden(),
                List.of(spec.key()));
    }

    @Test
    void registersThroughSinkAndReturnsRegistration() {
        List<String> sinkCalls = new ArrayList<>();
        ChemicalRegistrar registrar = new ChemicalRegistrar(spec -> {
            sinkCalls.add(spec.key());
            return registration(spec);
        });

        ChemicalRegistration result = registrar.register(gas("test_chemical"));

        assertEquals(List.of("gtsnlib:test_chemical"), sinkCalls);
        assertEquals("gtsnlib:test_chemical", result.key());
        assertEquals("gtsnlib:test_chemical", result.resourceLocation());
        assertEquals("test_chemical", result.id());
        assertEquals("gtsnlib", result.namespace());
        assertEquals(ChemicalKind.GAS, result.kind());
        assertEquals("mekanism:gas", result.registryId());
        assertEquals(0x88CCFF, result.tint());
        assertTrue(registrar.isRegistered("gtsnlib:test_chemical"));
        assertEquals(1, registrar.registrations().size());
    }

    @Test
    void rejectsDuplicateChemical() {
        AtomicInteger sinkCalls = new AtomicInteger();
        ChemicalRegistrar registrar = new ChemicalRegistrar(spec -> {
            sinkCalls.incrementAndGet();
            return registration(spec);
        });

        registrar.register(gas("test_chemical"));

        assertThrows(IllegalArgumentException.class, () -> registrar.register(gas("test_chemical")));
        assertEquals(1, sinkCalls.get(), "重复化学物质不得再次进入 sink");
    }

    @Test
    void invokesRegistrationHookAfterSinkRegistration() {
        List<ChemicalRegistration> hooked = new ArrayList<>();
        ChemicalSpec withHook = ChemicalSpec.builder("gtsnlib", "test_chemical")
                .kind(ChemicalKind.GAS)
                .registrationHook(hooked::add)
                .build();
        ChemicalRegistrar registrar = new ChemicalRegistrar(ChemicalRegistrarTest::registration);

        registrar.register(withHook);

        assertEquals(1, hooked.size());
        assertEquals("gtsnlib:test_chemical", hooked.get(0).key());
    }

    @Test
    void doesNotInvokeHookWhenSinkFails() {
        AtomicInteger hookCalls = new AtomicInteger();
        ChemicalSpec withHook = ChemicalSpec.builder("gtsnlib", "test_chemical")
                .kind(ChemicalKind.GAS)
                .registrationHook(registration -> hookCalls.incrementAndGet())
                .build();
        ChemicalRegistrar registrar = new ChemicalRegistrar(spec -> {
            throw new IllegalStateException("sink rejected");
        });

        assertThrows(IllegalStateException.class, () -> registrar.register(withHook));
        assertEquals(0, hookCalls.get());
        assertFalse(registrar.isRegistered("gtsnlib:test_chemical"));
        assertTrue(registrar.registrations().isEmpty());
    }

    @Test
    void reportsUnregisteredChemicalAsAbsent() {
        ChemicalRegistrar registrar = new ChemicalRegistrar(ChemicalRegistrarTest::registration);

        assertFalse(registrar.isRegistered("gtsnlib:test_chemical"));
        assertTrue(registrar.registration("gtsnlib:test_chemical").isEmpty());
    }

    @Test
    void slurryRegistrationCarriesBothDirtyAndCleanLocations() {
        ChemicalSpec slurry = ChemicalSpec.builder("gtsnlib", "star_alloy")
                .kind(ChemicalKind.SLURRY)
                .ore("gtsnlib", "star_alloy")
                .build();
        ChemicalRegistrar registrar = new ChemicalRegistrar(spec -> new ChemicalRegistration(
                spec.id(), spec.namespace(), spec.key(), spec.kind(), spec.kind().registryId(),
                spec.tint(), spec.hidden(), List.of(
                        spec.namespace() + ":dirty_" + spec.id(),
                        spec.namespace() + ":clean_" + spec.id())));

        ChemicalRegistration result = registrar.register(slurry);

        assertEquals(List.of("gtsnlib:dirty_star_alloy", "gtsnlib:clean_star_alloy"),
                result.resourceLocations());
        assertEquals("gtsnlib:dirty_star_alloy", result.resourceLocation());
        assertTrue(result.hasMultipleResourceLocations());
    }

    @Test
    void rejectsNullSpec() {
        ChemicalRegistrar registrar = new ChemicalRegistrar(ChemicalRegistrarTest::registration);
        assertThrows(NullPointerException.class, () -> registrar.register(null));
    }
}
