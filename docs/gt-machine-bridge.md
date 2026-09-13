# GT 机器界面桥接（方案 A：只读数据通道 + 自带 UI 组件，#22）

GTSNLib 提供一条**只读**通道，把 GTCEu（7.5.3）机器状态读进 GTSN 自研 UI，让依赖 mod 能用 GTSN UI
做机器仪表盘 / 监控面板 / 总线总览，而**无需接触 GTCEu 或 LDLib**。

> **边界（本里程碑不做）**：不接管 / 不替换 GT 原有的 LDLib 机器界面（方案 B，另立后续里程碑），
> 不引入 LDLib，不混入 LDLib 管线，不做双向交互（按钮 / 槽位操作）——本轮仅只读展示。

## 1. 数据模型（GT-free）

`com.gtsn.lib.api.GtMachineSnapshot`：某一时刻从 GT 机器读出的展示态值对象，**不含任何 GTCEu 类型**。

| 字段 | 说明 |
| --- | --- |
| `machineId` | 机器资源位置，例如 `gtsnlib:test_machine` |
| `tier` | GT 电压等级 |
| `status` | 配方逻辑状态文本（`IDLE` / `WORKING` / `WAITING` / `SUSPEND`；无配方逻辑时为 `UNKNOWN`） |
| `energyStored` / `energyCapacity` / `inputVoltage` | 能量容器的存量 / 容量 / 输入电压 |
| `progress` / `maxProgress` / `working` | 配方进度 / 总时长 / 是否运行中 |
| `itemSlots`（`List<ItemStack>`） | 物品槽内容，按槽位索引顺序，空槽为 `ItemStack.EMPTY` |
| `fluidTanks`（`List<FluidStack>`） | 流体罐内容，按罐索引顺序，空罐为 `FluidStack.EMPTY` |
| `fluidTankCapacities`（`List<Long>`，附加） | 与 `fluidTanks` 对齐的罐容量；缺省时 `fluidCapacity(i)` 回退 0 |

派生逻辑（纯算术，可无游戏环境单测）：`energyRatio()`、`progressRatio()`（均钳制到 `[0,1]`，分母 ≤ 0 时返回 0）、
`hasEnergy()`、`hasRecipeProgress()`、`fluidCapacity(int)`。

用 `GtMachineSnapshot.builder(machineId)…build()` 构建；`GtMachineSnapshot.empty()` 为保底占位。
快照不可变，列表访问器返回不可修改副本，**不持有机器引用**，因此不会反向操作机器。

## 2. 适配层（GTCEu 边界）

`com.gtsn.lib.gt.adapter.GtMachineSnapshots`（ADR-0005：GTCEu import 只允许出现在 `gt/adapter`）：

| 方法 | 用途 |
| --- | --- |
| `GtMachineSnapshot of(MetaMachine)` | 从服务端 / 客户端已有的镜像机器实例构建 |
| `Optional<GtMachineSnapshot> at(Level, BlockPos)` | 按坐标解析方块实体读取镜像机器并构建；无机器返回空 |
| `boolean isGtMachine(Level, BlockPos)` | 坐标处是否为 GT 机器（供开发入口判定） |

读取统一经 `GTCapabilityHelper`（可空安全）：`getEnergyContainer` / `getRecipeLogic` /
`getItemHandler` / `getFluidHandler`；机器身份与 tier 取 `MetaMachine#getDefinition()`。
缺失能力的机器返回 0 / 空列表，不失败。

### 调试写入（同步证据用）

`com.gtsn.lib.gt.adapter.GtMachineDebug.charge(Level, BlockPos, long)` 是适配层**唯一的写操作**：
向坐标处能量容器 `changeEnergy(delta)`，成功后调用 `MetaMachine#onChanged()` 触发 LDLib 推送。
（`NotifiableEnergyContainer.changeEnergy` 只改字段、不标脏，若不显式 `onChanged`，客户端的
`@DescSynced` 镜像永远停在旧值——这正是 #24 F1-1 暴露的同步证据缺口。）它只服务开发命令
`/gtsnlib debug charge <x> <y> <z> <eu>` 与自动测试；只读展示路径不使用。

### 客户端同步语义

GT 机器状态由 **LDLib 托管同步**（`IManaged` / `FieldManagedStorage` / `@Persisted` / `@DescSynced`）：
客户端方块实体持有**镜像 `MetaMachine`**，能量容器、配方逻辑等带 `@DescSynced` 的字段被同步到客户端。
因此：

- 客户端 `at(clientLevel, pos)` 读到的就是**同步后的值**，可逐帧 / 逐 tick 刷新，**无需自定义网络包**；
- 服务端读到的则是权威值（同一 API，服务端 / 客户端共用）；
- 若目标区块未加载 / 方块实体未同步，`at` 返回空。

## 3. UI 组件（复用现有主题角色与内核契约）

均位于 `com.gtsn.lib.ui.widget`，纯状态逻辑与渲染解耦，可无游戏环境测试；颜色默认取主题
`PROGRESS_*` / `ACCENT` / `BORDER` / `PANEL_*` 角色，随主题切换：

| 组件 | 状态逻辑 | 视觉 |
| --- | --- | --- |
| `EnergyBarWidget` | `energy(stored, capacity)`、`ratio()`（存量钳制到容量）、`labelText()` | 水平比例条，内置标签 `stored/capacity EU` |
| `TankWidget` | `tank(amount, capacity)`、`ratio()`、`fluidName()` | 垂直罐，自底向上填充（不持有 `FluidStack`，由界面翻译为存量 / 容量 / 名称） |
| `ProgressArrowWidget` | `progress(progress, maxProgress)`、`ratio()`、`working()` | 向右箭头，按比例裁剪填充；空闲（`working=false`）降低填充 alpha 变暗，区分运行 / 暂停 |
| `MachineSlotsPanel` | `slotCount()` / `columns()` / `rows()` / `icon(i)` / `slot(i)` | 图标网格（每格 `ItemSlotWidget`），空槽用 `null` 图标；`icon(i)` / `slot(i)` 越界统一抛 `IndexOutOfBoundsException` |
| `MachineStatusPanel` | 从 MC-free 的 `MachineStatusView` 装配上述组件 + 标题 / 状态文本 | `PanelWidget` 容器，标题为机器 id；`ui.widget` **无 MC 依赖** |

`MachineStatusView`（MC-free 视图模型，`ui.widget`）：槽位 = `Slot(index, SlotIcon)`（图标无关载荷，
空槽 `null`），流体罐 = `Tank(amount, capacity, name)`。`GtMachineSnapshot`（含 `ItemStack` /
`FluidStack`）→ 视图模型的翻译由**客户端 binder** `com.gtsn.lib.ui.client.MachineStatusViewBinder`
完成（`ItemStackIcon::of` 渲染真实物品图标、`FluidStack#getDisplayName` 取罐名），故组件层与 MC 类型解耦、
可在无游戏环境测试，同时依赖 mod 可用纯视图复用 `MachineStatusPanel`。

## 4. 机器状态界面与开发入口

- `com.gtsn.lib.ui.screen.MachineStatusScreen`（客户端）：只读展示一台机器的实时快照，每 5 tick 从
  客户端镜像机器重取快照并重建控件树。界面无可操作控件。文本度量绑定本屏**生效主题**字体
  （`GtsnScreen#textMetrics()`，含屏幕级覆盖），换行与渲染字体一致（#24 F2-1）。
- **`/gtsnui machine`**（客户端命令，本地执行、不发往服务器）：对玩家注视的 GT 机器（8 格内）打开该界面；
  未注视 GT 机器时给出提示。命令与自动测试接线见 `com.gtsn.lib.ui.client.GtsnUiClient`。
- **`/gtsnlib debug charge <x> <y> <z> <eu>`**（服务端命令，权限 ≥ 2）：调试 / 自动测试用，向坐标处机器
  能量容器注入（或移除）能量并触发客户端镜像同步；不用于正常游玩。

## 5. 自动测试（无人值守证据）

```powershell
$env:GTSNLIB_UI_AUTOTEST="machine"; .\gradlew.bat runClient; Remove-Item Env:\GTSNLIB_UI_AUTOTEST
```

客户端进入标题界面后固定窗口，创建 / 载入存档 `gtsnlib-machine-autotest`，服务端在玩家旁
`setblock gtsnlib:test_machine`。随后**服务端经集成服务端执行 `/gtsnlib debug charge`**：先把存量排空到 0，
等客户端镜像读到 0（清除复用存档里的历史值），再注入 512 EU，客户端轮询到 `energyStored() > 0` 才算通过；
超时即 FAIL（#24 F1-1）。然后打开 `MachineStatusScreen`，校验界面快照的机器 id / tier / 非零能量，抓图
`run/screenshots/gtsnlib-ui-machine-status.png` 并退出。

> **能量容量 2048 不是同步证据**：它是 `GTValues.V[tier]*64*amperage` 的 tier 派生值，两端一致；
> 只有「服务端注入后客户端从 0 变为非零」的跃迁才证明 `@DescSynced` 镜像读取。该断言可失败——
> 去掉注入（或去掉 `GtMachineDebug` 的 `onChanged`）时客户端保持 0，测试 RED。

## 6. 纪律与边界

- **GTCEu import 只在 `com.gtsn.lib.gt.adapter`**（ADR-0005）。验证：
  `grep -rn "import com.gregtechceu" src/main/java` 只命中适配层。
- **不依赖 LDLib**：库内无 `lowdragmc` / `ldlib` 引用。
- **不引入 GT 类型到 `api`**：`GtMachineSnapshot` 只含 MC 公共类型（`ItemStack` / `FluidStack`）。
- **客户端类不在专职服务端加载**：`MachineStatusScreen` / `GtsnUiClient` / `GtsnUiMachineAutotest` 均
  由 `@EventBusSubscriber(Dist.CLIENT)` 或客户端 Screen 链路限定；`runServer` 日志无客户端类加载。
- **只读展示**：`GtMachineSnapshots`（含界面链路）只调读取方法；界面无写回，不打开 GT 原生机器界面。
  唯一的适配层写操作是 `GtMachineDebug.charge`，仅服务调试命令 / 自动测试。
- **`ui.widget` 无 MC 依赖**（#24 F1-3）：机器面板经 MC-free 的 `MachineStatusView` 消费数据，
  MC 类型翻译收敛在客户端 binder `MachineStatusViewBinder` 与 `ItemStackIcon`。

## 7. 验证

```powershell
.\gradlew.bat build               # 编译 + 单测（快照模型 / 组件状态逻辑 headless）
.\gradlew.bat runGameTestServer   # GameTest：放置 gtsnlib:test_machine 并断言快照 id/tier/能量/进度
.\gradlew.bat runServer           # 专职服务端：无客户端类加载、无类链接错误
$env:GTSNLIB_UI_AUTOTEST="machine"; .\gradlew.bat runClient   # 端到端截图
```

证据留档：`docs/acceptance/gt-machine-bridge.txt` 与 `docs/acceptance/screenshots/gtsnlib-ui-machine-status.png`。

## 参考

- `docs/adr/0004-self-built-ui-framework.md`（自研 UI，不基于 LDLib）
- `docs/adr/0005-pin-gtceu-7.5.3-with-adapter.md`（适配层隔离）
- `docs/ui.md`（GTSN UI 内核 / 组件 / 主题 / 数据同步）
- `CONTEXT.md`（术语：GTSN UI、联动、注册简化层）
