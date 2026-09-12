package com.gtsn.lib.gt.registration;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * 通用注册器（#15）：声明式方块 / 物品 / 机器注册的统一入口。负责重复检测、方块物品冲突检测与
 * 钩子回调，实际翻译为 GTCEu 注册由注入的 {@link Sink} 完成（生产实现位于
 * {@link com.gtsn.lib.gt.adapter}）。
 *
 * <p>本类不依赖任何 Minecraft / Forge / GTCEu 类型，可注入假 {@link Sink} 完整单测。</p>
 *
 * <p>重复检测按种类各自进行；此外，当方块声明 {@link BlockSpec#withItem()} 时，其自动生成的方块物品
 * 会占用同名物品键——之后注册的同名独立物品会被拒绝；反过来，若同名物品已注册，则声明生成物品的同名
 * 方块同样会被拒绝。两个方向都校验，避免游戏内物品注册表冲突。</p>
 */
public final class RegistrationRegistrar {

    /** 把声明式规格翻译为真实 GTCEu 注册的端口；生产实现位于适配层。 */
    public interface Sink {

        /**
         * 注册方块并返回结果视图。
         *
         * @throws RuntimeException 注册失败时向上抛出，注册器不会回调钩子
         */
        BlockRegistration registerBlock(BlockSpec spec);

        /** 注册物品并返回结果视图；失败时向上抛出。 */
        ItemRegistration registerItem(ItemSpec spec);

        /** 注册机器并返回结果视图；失败时向上抛出。 */
        MachineRegistration registerMachine(MachineSpec spec);
    }

    private final Sink sink;
    private final Map<String, BlockRegistration> blocks = new LinkedHashMap<>();
    private final Map<String, ItemRegistration> items = new LinkedHashMap<>();
    private final Map<String, MachineRegistration> machines = new LinkedHashMap<>();
    private final Set<String> claimedItemKeys = new LinkedHashSet<>();

    public RegistrationRegistrar(Sink sink) {
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    /**
     * 注册一个方块：先做重复检测，再经 {@link Sink} 注册，最后回调钩子。
     *
     * @throws IllegalArgumentException 同一 {@code namespace:id} 的方块已注册，或声明生成物品时该物品键
     *                                  已被其它注册占用
     */
    public BlockRegistration registerBlock(BlockSpec spec) {
        Objects.requireNonNull(spec, "spec");
        String key = spec.key();
        if (blocks.containsKey(key)) {
            throw new IllegalArgumentException("block already registered: " + key);
        }
        if (spec.withItem() && claimedItemKeys.contains(key)) {
            throw new IllegalArgumentException(
                    "block's generated item already provided by another registration: " + key);
        }
        BlockRegistration registration = Objects.requireNonNull(sink.registerBlock(spec), "sink returned null");
        blocks.put(key, registration);
        if (registration.hasItem()) {
            claimedItemKeys.add(registration.itemResourceLocation());
        }
        spec.hook().ifPresent(hook -> hook.onRegistered(RegistrationKind.BLOCK, registration.resourceLocation()));
        return registration;
    }

    /**
     * 注册一个物品：先做重复检测（含方块自动生成物品的占用），再经 {@link Sink} 注册，最后回调钩子。
     *
     * @throws IllegalArgumentException 同一 {@code namespace:id} 的物品已注册，或与方块物品冲突
     */
    public ItemRegistration registerItem(ItemSpec spec) {
        Objects.requireNonNull(spec, "spec");
        String key = spec.key();
        if (items.containsKey(key)) {
            throw new IllegalArgumentException("item already registered: " + key);
        }
        if (claimedItemKeys.contains(key)) {
            throw new IllegalArgumentException(
                    "item already provided by a block's generated item: " + key);
        }
        ItemRegistration registration = Objects.requireNonNull(sink.registerItem(spec), "sink returned null");
        items.put(key, registration);
        claimedItemKeys.add(key);
        spec.hook().ifPresent(hook -> hook.onRegistered(RegistrationKind.ITEM, registration.resourceLocation()));
        return registration;
    }

    /**
     * 注册一台机器：先做重复检测，再经 {@link Sink} 注册，最后回调钩子。
     *
     * @throws IllegalArgumentException 同一 {@code namespace:id} 的机器已注册
     */
    public MachineRegistration registerMachine(MachineSpec spec) {
        Objects.requireNonNull(spec, "spec");
        String key = spec.key();
        if (machines.containsKey(key)) {
            throw new IllegalArgumentException("machine already registered: " + key);
        }
        MachineRegistration registration =
                Objects.requireNonNull(sink.registerMachine(spec), "sink returned null");
        machines.put(key, registration);
        spec.hook().ifPresent(hook -> hook.onRegistered(RegistrationKind.MACHINE, registration.resourceLocation()));
        return registration;
    }

    /** 给定种类与 {@code namespace:id} 是否已注册。 */
    public boolean isRegistered(RegistrationKind kind, String key) {
        Objects.requireNonNull(kind, "kind");
        return switch (kind) {
            case BLOCK -> blocks.containsKey(key);
            case ITEM -> items.containsKey(key);
            case MACHINE -> machines.containsKey(key);
        };
    }

    /** 按 {@code namespace:id} 查询方块注册结果。 */
    public Optional<BlockRegistration> block(String key) {
        return Optional.ofNullable(blocks.get(key));
    }

    /** 按 {@code namespace:id} 查询物品注册结果。 */
    public Optional<ItemRegistration> item(String key) {
        return Optional.ofNullable(items.get(key));
    }

    /** 按 {@code namespace:id} 查询机器注册结果。 */
    public Optional<MachineRegistration> machine(String key) {
        return Optional.ofNullable(machines.get(key));
    }

    /** 按注册顺序返回全部方块结果。 */
    public List<BlockRegistration> blocks() {
        return List.copyOf(blocks.values());
    }

    /** 按注册顺序返回全部物品结果。 */
    public List<ItemRegistration> items() {
        return List.copyOf(items.values());
    }

    /** 按注册顺序返回全部机器结果。 */
    public List<MachineRegistration> machines() {
        return List.copyOf(machines.values());
    }
}
