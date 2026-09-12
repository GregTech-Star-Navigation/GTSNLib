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
 * {@link RegistrationRegistrar} 行为验证（#15）：注入假 {@link RegistrationRegistrar.Sink}，不加载
 * GTCEu / Minecraft。
 *
 * <p>覆盖外部可观察行为：经 sink 注册并返回结果、重复注册拒绝（含方块自动生成物品与独立物品的冲突）、
 * 钩子在 sink 成功后回调、失败时不回调、已注册查询。</p>
 */
class RegistrationRegistrarTest {

    private static BlockSpec blockSpec(String id, boolean withItem) {
        return BlockSpec.builder("gtsnlib", id).withItem(withItem).build();
    }

    private static ItemSpec itemSpec(String id) {
        return ItemSpec.builder("gtsnlib", id).build();
    }

    private static MachineSpec machineSpec(String id) {
        return MachineSpec.builder("gtsnlib", id).build();
    }

    private static BlockRegistration blockResult(BlockSpec spec) {
        return new BlockRegistration(spec.id(), spec.namespace(), spec.key(),
                spec.withItem() ? spec.key() : "");
    }

    private static ItemRegistration itemResult(ItemSpec spec) {
        return new ItemRegistration(spec.id(), spec.namespace(), spec.key(), spec.maxStackSize());
    }

    private static MachineRegistration machineResult(MachineSpec spec) {
        return new MachineRegistration(spec.id(), spec.namespace(), spec.key(), spec.tier());
    }

    private static RegistrationRegistrar passthrough() {
        return new RegistrationRegistrar(new RegistrationRegistrar.Sink() {
            @Override
            public BlockRegistration registerBlock(BlockSpec spec) {
                return blockResult(spec);
            }

            @Override
            public ItemRegistration registerItem(ItemSpec spec) {
                return itemResult(spec);
            }

            @Override
            public MachineRegistration registerMachine(MachineSpec spec) {
                return machineResult(spec);
            }
        });
    }

    @Test
    void registersBlockThroughSinkAndReturnsRegistration() {
        List<String> sinkCalls = new ArrayList<>();
        RegistrationRegistrar registrar = new RegistrationRegistrar(new RegistrationRegistrar.Sink() {
            @Override
            public BlockRegistration registerBlock(BlockSpec spec) {
                sinkCalls.add(spec.key());
                return blockResult(spec);
            }

            @Override
            public ItemRegistration registerItem(ItemSpec spec) {
                throw new AssertionError("unexpected item registration");
            }

            @Override
            public MachineRegistration registerMachine(MachineSpec spec) {
                throw new AssertionError("unexpected machine registration");
            }
        });

        BlockRegistration result = registrar.registerBlock(blockSpec("test_block", true));

        assertEquals(List.of("gtsnlib:test_block"), sinkCalls);
        assertEquals("gtsnlib:test_block", result.resourceLocation());
        assertTrue(result.hasItem());
        assertTrue(registrar.isRegistered(RegistrationKind.BLOCK, "gtsnlib:test_block"));
        assertEquals(1, registrar.blocks().size());
    }

    @Test
    void registersItemAndMachineThroughSink() {
        RegistrationRegistrar registrar = passthrough();

        ItemRegistration item = registrar.registerItem(itemSpec("test_item"));
        MachineRegistration machine = registrar.registerMachine(machineSpec("test_machine"));

        assertEquals("gtsnlib:test_item", item.resourceLocation());
        assertEquals(64, item.maxStackSize());
        assertEquals("gtsnlib:test_machine", machine.resourceLocation());
        assertEquals(1, machine.tier());
        assertTrue(registrar.isRegistered(RegistrationKind.ITEM, "gtsnlib:test_item"));
        assertTrue(registrar.isRegistered(RegistrationKind.MACHINE, "gtsnlib:test_machine"));
    }

    @Test
    void rejectsDuplicateBlock() {
        AtomicInteger sinkCalls = new AtomicInteger();
        RegistrationRegistrar registrar = new RegistrationRegistrar(new RegistrationRegistrar.Sink() {
            @Override
            public BlockRegistration registerBlock(BlockSpec spec) {
                sinkCalls.incrementAndGet();
                return blockResult(spec);
            }

            @Override
            public ItemRegistration registerItem(ItemSpec spec) {
                return itemResult(spec);
            }

            @Override
            public MachineRegistration registerMachine(MachineSpec spec) {
                return machineResult(spec);
            }
        });

        registrar.registerBlock(blockSpec("test_block", true));

        assertThrows(IllegalArgumentException.class,
                () -> registrar.registerBlock(blockSpec("test_block", true)));
        assertEquals(1, sinkCalls.get(), "重复方块不得再次进入 sink");
    }

    @Test
    void rejectsDuplicateItemAndMachine() {
        RegistrationRegistrar registrar = passthrough();
        registrar.registerItem(itemSpec("test_item"));
        registrar.registerMachine(machineSpec("test_machine"));

        assertThrows(IllegalArgumentException.class, () -> registrar.registerItem(itemSpec("test_item")));
        assertThrows(IllegalArgumentException.class, () -> registrar.registerMachine(machineSpec("test_machine")));
    }

    @Test
    void rejectsItemCollidingWithGeneratedBlockItem() {
        AtomicInteger itemSinkCalls = new AtomicInteger();
        RegistrationRegistrar registrar = new RegistrationRegistrar(new RegistrationRegistrar.Sink() {
            @Override
            public BlockRegistration registerBlock(BlockSpec spec) {
                return blockResult(spec);
            }

            @Override
            public ItemRegistration registerItem(ItemSpec spec) {
                itemSinkCalls.incrementAndGet();
                return itemResult(spec);
            }

            @Override
            public MachineRegistration registerMachine(MachineSpec spec) {
                return machineResult(spec);
            }
        });

        registrar.registerBlock(blockSpec("shared", true));

        assertThrows(IllegalArgumentException.class, () -> registrar.registerItem(itemSpec("shared")));
        assertEquals(0, itemSinkCalls.get(), "与方块物品冲突的独立物品不得进入 sink");
    }

    @Test
    void rejectsBlockWhoseGeneratedItemCollidesWithExistingItem() {
        AtomicInteger blockSinkCalls = new AtomicInteger();
        RegistrationRegistrar registrar = new RegistrationRegistrar(new RegistrationRegistrar.Sink() {
            @Override
            public BlockRegistration registerBlock(BlockSpec spec) {
                blockSinkCalls.incrementAndGet();
                return blockResult(spec);
            }

            @Override
            public ItemRegistration registerItem(ItemSpec spec) {
                return itemResult(spec);
            }

            @Override
            public MachineRegistration registerMachine(MachineSpec spec) {
                return machineResult(spec);
            }
        });

        registrar.registerItem(itemSpec("shared"));

        assertThrows(IllegalArgumentException.class, () -> registrar.registerBlock(blockSpec("shared", true)));
        assertEquals(0, blockSinkCalls.get(), "与已有物品冲突的方块物品不得进入 sink");
        assertTrue(registrar.blocks().isEmpty());
    }

    @Test
    void allowsItemReusingBlockIdWhenBlockDoesNotGenerateItem() {
        RegistrationRegistrar registrar = passthrough();

        registrar.registerBlock(blockSpec("shared", false));
        ItemRegistration item = registrar.registerItem(itemSpec("shared"));

        assertEquals("gtsnlib:shared", item.resourceLocation());
    }

    @Test
    void invokesHookAfterSinkRegistration() {
        List<String> hooked = new ArrayList<>();
        BlockSpec withHook = BlockSpec.builder("gtsnlib", "test_block")
                .hook((kind, id) -> hooked.add(kind.key() + "=" + id))
                .build();
        RegistrationRegistrar registrar = passthrough();

        registrar.registerBlock(withHook);

        assertEquals(List.of("block=gtsnlib:test_block"), hooked);
    }

    @Test
    void doesNotInvokeHookWhenSinkFails() {
        AtomicInteger hookCalls = new AtomicInteger();
        BlockSpec withHook = BlockSpec.builder("gtsnlib", "test_block")
                .hook((kind, id) -> hookCalls.incrementAndGet())
                .build();
        RegistrationRegistrar registrar = new RegistrationRegistrar(new RegistrationRegistrar.Sink() {
            @Override
            public BlockRegistration registerBlock(BlockSpec spec) {
                throw new IllegalStateException("sink rejected");
            }

            @Override
            public ItemRegistration registerItem(ItemSpec spec) {
                return itemResult(spec);
            }

            @Override
            public MachineRegistration registerMachine(MachineSpec spec) {
                return machineResult(spec);
            }
        });

        assertThrows(IllegalStateException.class, () -> registrar.registerBlock(withHook));
        assertEquals(0, hookCalls.get());
        assertFalse(registrar.isRegistered(RegistrationKind.BLOCK, "gtsnlib:test_block"));
        assertTrue(registrar.blocks().isEmpty());
    }

    @Test
    void reportsUnregisteredAsAbsent() {
        RegistrationRegistrar registrar = passthrough();

        assertFalse(registrar.isRegistered(RegistrationKind.BLOCK, "gtsnlib:test_block"));
        assertTrue(registrar.block("gtsnlib:test_block").isEmpty());
        assertTrue(registrar.item("gtsnlib:test_item").isEmpty());
        assertTrue(registrar.machine("gtsnlib:test_machine").isEmpty());
    }

    @Test
    void registrationKindResolvesKey() {
        assertEquals(RegistrationKind.BLOCK, RegistrationKind.fromKey("block"));
        assertEquals(RegistrationKind.ITEM, RegistrationKind.fromKey("Item"));
        assertEquals(RegistrationKind.MACHINE, RegistrationKind.fromKey("machine"));
        assertThrows(IllegalArgumentException.class, () -> RegistrationKind.fromKey("unobtainium"));
    }
}
