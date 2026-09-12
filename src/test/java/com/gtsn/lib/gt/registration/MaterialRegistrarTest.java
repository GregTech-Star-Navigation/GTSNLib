package com.gtsn.lib.gt.registration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link MaterialRegistrar} 行为验证：注入假 {@link MaterialRegistrar.Sink}，不加载 GTCEu。
 *
 * <p>覆盖外部可观察行为：经 sink 注册并返回结果、重复材料拒绝、配方钩子在注册后回调、
 * 注册失败时不回调钩子、已注册查询。</p>
 */
class MaterialRegistrarTest {

    private static MaterialSpec spec(String id) {
        return MaterialSpec.builder("gtsnlib", id)
                .color(0x00FF00)
                .parts(MaterialPart.INGOT, MaterialPart.PLATE)
                .build();
    }

    private static MaterialRegistration registration(MaterialSpec spec) {
        return new MaterialRegistration(
                spec.id(),
                spec.namespace(),
                spec.key(),
                Map.of("ingot", spec.id() + "_ingot", "plate", spec.id() + "_plate"),
                List.of("ingotGtsnlibTest", "plateGtsnlibTest"));
    }

    @Test
    void registersThroughSinkAndReturnsRegistration() {
        List<String> sinkCalls = new ArrayList<>();
        MaterialRegistrar registrar = new MaterialRegistrar(spec -> {
            sinkCalls.add(spec.key());
            return registration(spec);
        });

        MaterialRegistration result = registrar.register(spec("stellar_alloy"));

        assertEquals(List.of("gtsnlib:stellar_alloy"), sinkCalls);
        assertEquals("gtsnlib:stellar_alloy", result.resourceLocation());
        assertEquals("stellar_alloy", result.id());
        assertEquals("gtsnlib", result.namespace());
        assertTrue(registrar.isRegistered("gtsnlib:stellar_alloy"));
        assertEquals(1, registrar.registrations().size());
        assertEquals(List.of("ingotGtsnlibTest", "plateGtsnlibTest"), result.oreTags());
        assertEquals("stellar_alloy_ingot", result.derivedItem("ingot").orElseThrow());
    }

    @Test
    void rejectsDuplicateMaterial() {
        AtomicInteger sinkCalls = new AtomicInteger();
        MaterialRegistrar registrar = new MaterialRegistrar(spec -> {
            sinkCalls.incrementAndGet();
            return registration(spec);
        });

        registrar.register(spec("stellar_alloy"));

        assertThrows(IllegalArgumentException.class, () -> registrar.register(spec("stellar_alloy")));
        assertEquals(1, sinkCalls.get(), "重复材料不得再次进入 sink");
    }

    @Test
    void invokesRecipeHookAfterSinkRegistration() {
        List<MaterialRegistration> hooked = new ArrayList<>();
        MaterialSpec withHook = MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .parts(MaterialPart.INGOT)
                .recipeHook(hooked::add)
                .build();
        MaterialRegistrar registrar = new MaterialRegistrar(MaterialRegistrarTest::registration);

        registrar.register(withHook);

        assertEquals(1, hooked.size());
        assertEquals("gtsnlib:stellar_alloy", hooked.get(0).resourceLocation());
    }

    @Test
    void doesNotInvokeRecipeHookWhenSinkFails() {
        AtomicInteger hookCalls = new AtomicInteger();
        MaterialSpec withHook = MaterialSpec.builder("gtsnlib", "stellar_alloy")
                .parts(MaterialPart.INGOT)
                .recipeHook(registration -> hookCalls.incrementAndGet())
                .build();
        MaterialRegistrar registrar = new MaterialRegistrar(spec -> {
            throw new IllegalStateException("sink rejected");
        });

        assertThrows(IllegalStateException.class, () -> registrar.register(withHook));
        assertEquals(0, hookCalls.get());
        assertFalse(registrar.isRegistered("gtsnlib:stellar_alloy"));
        assertTrue(registrar.registrations().isEmpty());
    }

    @Test
    void reportsUnregisteredMaterialAsAbsent() {
        MaterialRegistrar registrar = new MaterialRegistrar(MaterialRegistrarTest::registration);

        assertFalse(registrar.isRegistered("gtsnlib:stellar_alloy"));
        assertTrue(registrar.registration("gtsnlib:stellar_alloy").isEmpty());
    }
}
