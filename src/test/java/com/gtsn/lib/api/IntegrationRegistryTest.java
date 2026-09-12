package com.gtsn.lib.api;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link IntegrationRegistry} 行为验证：注入假 {@link ModPresence}，不依赖任何 Forge 类型。
 *
 * <p>覆盖外部可观察行为：在场初始化、缺席不实例化、单模块失败隔离、注册表查询。</p>
 */
class IntegrationRegistryTest {

    /** 记录初始化顺序、可选地让 init() 抛异常的假模块。 */
    private static final class FakeModule implements IntegrationModule {

        private final String modId;
        private final List<String> initOrder;
        private final boolean failOnInit;

        FakeModule(String modId, List<String> initOrder, boolean failOnInit) {
            this.modId = modId;
            this.initOrder = initOrder;
            this.failOnInit = failOnInit;
        }

        @Override
        public String modId() {
            return modId;
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public void init() {
            if (failOnInit) {
                throw new IllegalStateException("init failed: " + modId);
            }
            initOrder.add(modId);
        }
    }

    /** 类加载即失败的假模块，用来模拟“目标 mod 缺席时类型不可链接”。 */
    private static final class ExplodingModule implements IntegrationModule {

        static {
            if (Boolean.parseBoolean("true")) {
                throw new ExceptionInInitializerError("target mod type is absent");
            }
        }

        @Override
        public String modId() {
            return "broken";
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public void init() {
            // never reached
        }
    }

    /** init() 抛出非 LinkageError 的 JVM 级致命错误（OOM / StackOverflow 等的替身）的假模块。 */
    private static final class FatalModule implements IntegrationModule {

        @Override
        public String modId() {
            return "fatal";
        }

        @Override
        public boolean isPresent() {
            return true;
        }

        @Override
        public void init() {
            throw new AssertionError("fatal JVM error");
        }
    }

    @Test
    void presentModuleIsInitialized() {
        List<String> order = new ArrayList<>();
        IntegrationRegistry registry = new IntegrationRegistry(modId -> true);
        registry.register("mekanism", () -> () -> new FakeModule("mekanism", order, false));

        int initialized = registry.initializeAll();

        assertEquals(1, initialized);
        assertEquals(List.of("mekanism"), order);
        assertEquals(Optional.of("mekanism"), registry.get("mekanism").map(IntegrationModule::modId));
    }

    @Test
    void absentModuleIsNeverInstantiated() {
        AtomicInteger outerSupplierCalls = new AtomicInteger();
        AtomicInteger innerSupplierCalls = new AtomicInteger();
        IntegrationRegistry registry = new IntegrationRegistry(modId -> false);
        registry.register("create", () -> {
            outerSupplierCalls.incrementAndGet();
            return () -> {
                innerSupplierCalls.incrementAndGet();
                return new FakeModule("create", new ArrayList<>(), false);
            };
        });

        int initialized = registry.initializeAll();

        assertEquals(0, initialized);
        assertEquals(0, outerSupplierCalls.get(), "缺席时外层 supplier 不应被调用");
        assertEquals(0, innerSupplierCalls.get(), "缺席时内层 supplier 不应被调用");
        assertTrue(registry.modules().isEmpty());
        assertEquals(Optional.empty(), registry.get("create"));
    }

    @Test
    void failingModuleDoesNotBreakOthers() {
        List<String> order = new ArrayList<>();
        IntegrationRegistry registry = new IntegrationRegistry(modId -> true);
        registry.register("bad", () -> () -> new FakeModule("bad", order, true));
        registry.register("good", () -> () -> new FakeModule("good", order, false));

        int initialized = registry.initializeAll();

        assertEquals(1, initialized);
        assertEquals(List.of("good"), order, "失败模块不应阻断后续模块初始化");
        assertTrue(registry.failures().containsKey("bad"));
        assertTrue(registry.get("good").isPresent());
    }

    @Test
    void classLoadFailureIsIsolated() {
        List<String> order = new ArrayList<>();
        IntegrationRegistry registry = new IntegrationRegistry(modId -> true);
        registry.register("broken", () -> () -> new ExplodingModule());
        registry.register("good", () -> () -> new FakeModule("good", order, false));

        int initialized = registry.initializeAll();

        assertEquals(1, initialized);
        assertEquals(List.of("good"), order, "类加载失败的模块不应阻断其它模块");
        assertTrue(registry.failures().containsKey("broken"));
        assertTrue(registry.get("good").isPresent());
    }

    @Test
    void fatalJvmErrorPropagatesAndIsNotRecordedAsFailure() {
        List<String> order = new ArrayList<>();
        IntegrationRegistry registry = new IntegrationRegistry(modId -> true);
        registry.register("fatal", () -> () -> new FatalModule());
        registry.register("good", () -> () -> new FakeModule("good", order, false));

        AssertionError thrown = assertThrows(AssertionError.class, registry::initializeAll);

        assertEquals("fatal JVM error", thrown.getMessage());
        assertFalse(registry.failures().containsKey("fatal"), "JVM 级致命错误不得被当作普通联动失败记录");
        assertTrue(registry.failures().isEmpty(), "致命错误不应写入 failures()");
        assertTrue(order.isEmpty(), "致命错误应中止本次初始化，后续模块不再初始化");
    }

    @Test
    void queryReturnsExpectedModules() {
        List<String> order = new ArrayList<>();
        IntegrationRegistry registry = new IntegrationRegistry(modId -> !modId.equals("absent"));
        registry.addTargets(List.of("mekanism", "create", "absent"));
        registry.register("mekanism", () -> () -> new FakeModule("mekanism", order, false));
        registry.register("create", () -> () -> new FakeModule("create", order, false));

        registry.initializeAll();

        assertEquals(2, registry.registeredCount());
        assertEquals(List.of("mekanism", "create"), registry.modules().stream().map(IntegrationModule::modId).toList());
        assertEquals(List.of("mekanism", "create", "absent"), registry.targets());
        assertTrue(registry.isTarget("create"));
        assertTrue(registry.isPresent("mekanism"));
        assertFalse(registry.isPresent("absent"));
        assertTrue(registry.isRegistered("create"));
        assertFalse(registry.isRegistered("absent"));
        assertEquals(Optional.empty(), registry.get("absent"));
    }

    @Test
    void initializeAllIsIdempotent() {
        List<String> order = new ArrayList<>();
        IntegrationRegistry registry = new IntegrationRegistry(modId -> true);
        registry.register("ae2", () -> () -> new FakeModule("ae2", order, false));

        int first = registry.initializeAll();
        int second = registry.initializeAll();

        assertEquals(1, first);
        assertEquals(0, second, "已初始化的模块不应重复初始化");
        assertEquals(List.of("ae2"), order);
    }
}
