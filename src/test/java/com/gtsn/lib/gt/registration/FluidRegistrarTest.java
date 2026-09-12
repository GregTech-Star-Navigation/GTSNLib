package com.gtsn.lib.gt.registration;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link FluidRegistrar} 行为验证：注入假 sink，不加载 GTCEu（#13）。
 *
 * <p>覆盖外部可观察行为：经 sink 注册并返回结果、重复流体拒绝、注册钩子在 sink 成功后回调、
 * 失败时不回调、已注册查询。</p>
 */
class FluidRegistrarTest {

    private static FluidSpec spec(String id) {
        return FluidSpec.builder("gtsnlib", id)
                .standalone()
                .state(FluidState.LIQUID)
                .color(0x00FF00)
                .temperature(300)
                .build();
    }

    private static FluidRegistration registration(FluidSpec spec) {
        return new FluidRegistration(
                spec.id(),
                spec.namespace(),
                spec.key(),
                spec.state(),
                spec.material().map(FluidSpec.MaterialLink::key).orElse(""),
                spec.key());
    }

    @Test
    void registersThroughSinkAndReturnsRegistration() {
        List<String> sinkCalls = new ArrayList<>();
        FluidRegistrar registrar = new FluidRegistrar(spec -> {
            sinkCalls.add(spec.key());
            return registration(spec);
        });

        FluidRegistration result = registrar.register(spec("liquid_air"));

        assertEquals(List.of("gtsnlib:liquid_air"), sinkCalls);
        assertEquals("gtsnlib:liquid_air", result.resourceLocation());
        assertEquals("liquid_air", result.id());
        assertEquals("gtsnlib", result.namespace());
        assertEquals(FluidState.LIQUID, result.state());
        assertTrue(result.standalone());
        assertTrue(registrar.isRegistered("gtsnlib:liquid_air"));
        assertEquals(1, registrar.registrations().size());
    }

    @Test
    void rejectsDuplicateFluid() {
        AtomicInteger sinkCalls = new AtomicInteger();
        FluidRegistrar registrar = new FluidRegistrar(spec -> {
            sinkCalls.incrementAndGet();
            return registration(spec);
        });

        registrar.register(spec("liquid_air"));

        assertThrows(IllegalArgumentException.class, () -> registrar.register(spec("liquid_air")));
        assertEquals(1, sinkCalls.get(), "重复流体不得再次进入 sink");
    }

    @Test
    void invokesRegistrationHookAfterSinkRegistration() {
        List<FluidRegistration> hooked = new ArrayList<>();
        FluidSpec withHook = FluidSpec.builder("gtsnlib", "liquid_air")
                .standalone()
                .state(FluidState.LIQUID)
                .registrationHook(hooked::add)
                .build();
        FluidRegistrar registrar = new FluidRegistrar(FluidRegistrarTest::registration);

        registrar.register(withHook);

        assertEquals(1, hooked.size());
        assertEquals("gtsnlib:liquid_air", hooked.get(0).resourceLocation());
    }

    @Test
    void doesNotInvokeHookWhenSinkFails() {
        AtomicInteger hookCalls = new AtomicInteger();
        FluidSpec withHook = FluidSpec.builder("gtsnlib", "liquid_air")
                .standalone()
                .state(FluidState.LIQUID)
                .registrationHook(registration -> hookCalls.incrementAndGet())
                .build();
        FluidRegistrar registrar = new FluidRegistrar(spec -> {
            throw new IllegalStateException("sink rejected");
        });

        assertThrows(IllegalStateException.class, () -> registrar.register(withHook));
        assertEquals(0, hookCalls.get());
        assertFalse(registrar.isRegistered("gtsnlib:liquid_air"));
        assertTrue(registrar.registrations().isEmpty());
    }

    @Test
    void reportsUnregisteredFluidAsAbsent() {
        FluidRegistrar registrar = new FluidRegistrar(FluidRegistrarTest::registration);

        assertFalse(registrar.isRegistered("gtsnlib:liquid_air"));
        assertTrue(registrar.registration("gtsnlib:liquid_air").isEmpty());
    }

    @Test
    void materialLinkedRegistrationExposesMaterialKey() {
        FluidSpec linked = FluidSpec.builder("gtsnlib", "star_alloy_gas")
                .material("gtsnlib", "star_alloy")
                .state(FluidState.GAS)
                .build();
        FluidRegistrar registrar = new FluidRegistrar(FluidRegistrarTest::registration);

        FluidRegistration result = registrar.register(linked);

        assertFalse(result.standalone());
        assertEquals("gtsnlib:star_alloy", result.materialKey());
    }
}
