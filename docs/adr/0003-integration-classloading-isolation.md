# 联动代码按目标 mod 隔离并延迟加载

每个联动目标一个独立包（`com.gtsn.lib.integration.<mod>`）。仅由**不含任何可选类型引用**的门面类，经 `ModList`/`FMLLoader.getLoadingModList()` 检测后实例化；实例化采用 `Supplier<Supplier<T>>` 双层 supplier 模式，确保目标 mod 缺席时相关类不被 JVM 链接，杜绝 `NoClassDefFoundError`。

**Considered Options**: 直接在 `@Mod` 主类或 `@Mod.EventBusSubscriber` 类中引用可选类型（此类会被强制加载，缺席时必崩）。

**Consequences**: 联动模块可安全增删；纪律要求：门面/事件订阅类禁止出现任何可选 mod 的类型（字段、方法签名、父类、注解值）。
