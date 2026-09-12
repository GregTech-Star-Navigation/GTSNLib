# 注册前置条件：命名空间材料注册表

GTSNLib 的注册简化层（材料 / 流体 / 方块 / 物品 / 机器）在底层都落到 GTCEu 的**命名空间材料注册表**
（`MaterialRegistry`）与它自带的 `GTRegistrate`。因此，附属 mod 在使用这些 helper 之前，**必须**先为自己的
ModID 建立一个 GTCEu 材料注册表——否则 `GtAdapter.registrate(modId)` 会抛出可诊断错误：

```
no GTCEu material registry for namespace 'mymod'. ...
Handle MaterialRegistryEvent on the mod event bus and call
GtAdapter.get().createRegistrate("mymod") ...
```

> 注意：即使只使用方块 / 物品 / 机器 helper（不注册材料），也必须建立该注册表。这些 helper 经
> `GtceBackend.registrate(modId)` → `requireRegistry(modId)` 取命名空间的 registrate；GTCEu 只为已建立的
> 材料注册表挂接 `RegisterEvent`，孤立的 `GTRegistrate.create(modId)` 没有挂事件总线，条目不会真正注册。

## GTCEu 的注册时序（7.5.3，已核对源码）

GTCEu 在 `CommonProxy#init()`（`FMLConstructModEvent` 之后）依次：

1. 发出 **`MaterialRegistryEvent`** —— **唯一**允许 `GTCEuAPI.materialManager.createRegistry(modId)` 的阶段
   （registry manager 的 `Phase.PRE`）。
2. 解冻注册表，注册 GT 自身材料。
3. 发出 **`MaterialEvent`** —— 附属 mod 在此声明材料与（经 registrate 排队的）方块 / 物品。
4. 关闭并冻结。

机器是例外：`GTRegistries.MACHINES` 在 GTCEu 自身机器注册期间的 `GTCEuAPI.RegisterEvent<MachineDefinition>`
窗口内才未冻结，附属 mod 必须在此时机经适配层 `registerMachine(...)` 注册。

## 推荐写法（附属 mod）

在 `@Mod` 事件总线上订阅 `MaterialRegistryEvent`，并在其中建立注册表：

```java
@Mod.EventBusSubscriber(modid = MyMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class MyRegistrations {

    // 必须在 MaterialRegistryEvent 中建立命名空间材料注册表；helper 返回的 registrate 可自行保留。
    @SubscribeEvent
    public static void onMaterialRegistry(MaterialRegistryEvent event) {
        GtAdapter.get().createRegistrate(MyMod.MOD_ID);
    }

    @SubscribeEvent
    public static void onMaterial(MaterialEvent event) {
        GtAdapter.get().registerBlock(
                BlockSpec.builder(MyMod.MOD_ID, "my_block").strength(2F, 3F).build());
        // registerItem / registerMachine / registerMaterial / registerFluid 同理。
    }
}
```

- `GtAdapter.createRegistrate(modId)` 只在 `MaterialRegistryEvent` 期间调用一次；重复调用会因注册表已存在而报错。
- 机器经 `GtAdapter.get().registerMachine(...)` 注册，但须发生在 GTCEu 的
  `RegisterEvent<MachineDefinition>` 窗口内（`mods.toml` 中 GTSNLib 对 `gtceu` 声明 `ordering="AFTER"` 只是加载顺序，
  不代替上述事件时机）。
- 完整类型与返回值见 `com.gtsn.lib.gt.registration` 包内各 `*Spec` / `*Registration` 类型。

## 适配层纪律

`GtAdapter` 只暴露 GTSN 自有类型；GTCEu 类型（含 `MaterialRegistry` / `GTRegistrate`）不越过
`com.gtsn.lib.gt.adapter` 边界（ADR-0005）。上游 8.0 移除 `materialManager` / `MaterialRegistryEvent` /
`registerRegistrate` 时，迁移点收敛在适配层内部。

## 参考

- `docs/adr/0005-pin-gtceu-7.5.3-with-adapter.md`
- `CONTEXT.md`（术语：命名空间材料注册表）
- `docs/ui.md`（自研 UI 框架；与注册简化层无关）
