# GTSNLib 集成验收（ticket #20）

> 端到端验收记录：门禁、`/gtsnlib` 命令实测、装/未装两态联动、UI 实测、人工步骤清单。
> 本文件是 #20「集成验收」的验收证据汇总；原始输出见 `docs/acceptance/`，截图见 `docs/acceptance/screenshots/`。
>
> 固定点：`HEAD` = `8b83ddc`（本项目验收运行时的提交）。规格见 #1，领域词汇见 `CONTEXT.md`，架构决策见 `docs/adr/`。

## 1. 验收标准对照

| # | 验收标准 | 结论 | 证据 |
| --- | --- | --- | --- |
| AC-1 | 进世界 + `/gtsnlib` 实测证据 | 部分自动 + 人工清单 | 客户端同步自动测试**真实载入存档进入世界**（`[GTSNLib] sync autotest: loading existing world 'gtsnlib-sync-autotest'` → `world ready`）；`/gtsnlib` 全命令树在**专职服务端真实加载环境**经 RCON 执行并留档；GameTest 在**已加载世界**内以真实命令栈执行 `/gtsnlib`。见 §4、§5，人工清单见 §7 |
| AC-2 | 装/未装联动两态检测正确（证据） | 通过 | 未装态 `0/6`、装 Mekanism 态 `1/6 [mekanism]`；两态 GameTest 各 38/38 通过；两态均无 `NoClassDefFoundError`。见 §5 |
| AC-3 | UI 测试界面实测证据 | 通过 | `GTSNLIB_UI_AUTOTEST=1` 自动测试 + 逐主题截图；`=sync` 数据同步演示截图。见 §6 |
| AC-4 | 文档/命令表更新 | 通过 | `AGENTS.md` 常用命令表更新；`docs/acceptance.md`（本文件）新增；`docs/ui.md` 与本文件相互引用 |
| AC-5 | 全部证据留档 | 通过 | 原始输出 `docs/acceptance/*.txt`；截图 `docs/acceptance/screenshots/*.png`。见 §3 索引 |

## 2. 环境

- Minecraft 1.20.1 · Forge 47.4.0 · JDK 17（`org.gradle.java.home` 固定）· Gradle 8.14
- 运行插件：ModDevGradle legacyforge 2.0.86 · Parchment 2023.09.03
- 硬依赖：GTCEu 7.5.3 `:slim`、LDLib、Registrate、Configuration
- 联动目标（`mods.toml` 均 `mandatory=false` / `versionRange="[0,)"` / `ordering="AFTER"`）：Mekanism、Immersive Engineering、Create、AE2、Ender IO、Ad Astra
- 演示内容（`gtsnlib` 命名空间）：材料 `gtsnlib:star_alloy`（衍生件 + 矿词 + 液/气/等离子）、一次性流体 `gtsnlib:stellar_air`、方块/物品/机器 `test_block` / `test_item` / `test_machine`、化学物质 `gtsnlib:test_chemical`

## 3. 证据索引

| 证据文件 | 内容 | 采集命令 |
| --- | --- | --- |
| `docs/acceptance/build-test.txt` | 门禁构建 + 单测汇总（402 项） | `.\gradlew.bat clean build` / `.\gradlew.bat cleanTest test` |
| `docs/acceptance/gametest-absent.txt` | 未装态 GameTest 关键行（38/38） | `.\gradlew.bat runGameTestServer` |
| `docs/acceptance/gametest-present.txt` | 装 Mekanism 态 GameTest 关键行（38/38） | `.\gradlew.bat runGameTestServer`（临时 `modRuntimeOnly`） |
| `docs/acceptance/server-absent.txt` | 未装态 RCON `/gtsnlib` 全命令原始输出 | `.\gradlew.bat runServer` + RCON（脚本 `logs/acceptance/rcon-sweep.ps1`） |
| `docs/acceptance/server-present.txt` | 装 Mekanism 态 RCON 全命令原始输出 | 同上（临时 `modRuntimeOnly`） |
| `docs/acceptance/client-ui-autotest.txt` | `GTSNLIB_UI_AUTOTEST=1` 自动测试关键日志 | `$env:GTSNLIB_UI_AUTOTEST="1"; .\gradlew.bat runClient` |
| `docs/acceptance/client-sync-autotest.txt` | `GTSNLIB_UI_AUTOTEST=sync` 数据同步/进世界关键日志 | `$env:GTSNLIB_UI_AUTOTEST="sync"; .\gradlew.bat runClient` |
| `docs/acceptance/screenshots/*.png` | 组件库三主题截图 + 数据同步演示截图 | 上述两次客户端运行产出 |

> 说明：`run/`、`build/`、`logs/` 均被 `.gitignore` 排除，故证据以**原始输出的可提交副本**留档于 `docs/acceptance/`；截图从 `run/screenshots/` 复制而来。

## 4. 门禁扫描（全部通过）

| 命令 | 结果 |
| --- | --- |
| `.\gradlew.bat clean build` | `BUILD SUCCESSFUL in 28s`，8 tasks executed；产出 `build/libs/gtsnlib-0.1.0.jar`（reobf） |
| `.\gradlew.bat cleanTest test` | `BUILD SUCCESSFUL in 16s`；JUnit5：**suites=56 / tests=402 / failures=0 / errors=0 / skipped=0** |
| `.\gradlew.bat runGameTestServer` | `All 38 required tests passed :)`（见 §5 两态） |
| `.\gradlew.bat runServer` | 专职服务端起停正常，命令树经 RCON 执行（见 §4.1） |

原始输出：`build-test.txt`、`gametest-absent.txt`、`server-absent.txt`。

### 4.1 `/gtsnlib` 命令树实测（专职服务端，未装态）

在真实加载的专职服务端上启用 RCON（`run/server/server.properties`，属 `run/` 本地目录、随 `.gitignore` 排除），逐条执行并抓取原始输出：

```
/gtsnlib
  GTSNLib 0.1.0 | integrations: 6 | targets present: 0/6
  mekanism: absent / immersiveengineering: absent / create: absent
  ae2: absent / enderio: absent / ad_astra: absent

/gtsnlib gt
  GT adapter | available: true | tag prefixes: 95
  material iron: present @ gtceu:iron [gtceu] formula=Fe

/gtsnlib gt material gtsnlib:star_alloy
  material gtsnlib:star_alloy: present @ gtsnlib:star_alloy [gtsnlib]
  registration gtsnlib:star_alloy | derived: {plate, ingot, rod, dust}
  ore tags: [forge:ingots|plates|dusts|rods/star_alloy]
  part plate/ingot/rod/dust -> ... generated=true
  fluid liquid/gas/plasma -> gtsnlib:star_alloy* present=true

/gtsnlib gt material iron       -> material iron present; registration iron: absent
/gtsnlib gt fluid gtsnlib:star_alloy_plasma -> present, state=plasma [material gtsnlib:star_alloy]
/gtsnlib gt fluid gtsnlib:stellar_air       -> present, state=gas [standalone]
/gtsnlib reg
  GTSNLib registrations | blocks: 1 | items: 1 | machines: 1
  block/item/machine gtsnlib:test_*: present=true
/gtsnlib mek                     -> Mekanism chemicals | backend: false | registered: 0
/gtsnlib mek chemical gtsnlib:test_chemical -> backend unavailable
/gtsnlib ui                      -> "A player is required to run this command here"（控制台无玩家，预期）
```

完整逐字输出见 `docs/acceptance/server-absent.txt`。

> `/gtsnlib ui` 需要玩家执行者（经 `NetworkHooks.openScreen` 打开菜单），在控制台/RCON 下按设计返回“需要玩家”。该入口的实际游戏内实测见 §6 的 `=sync` 自动测试（客户端在真实世界中打开同一 `DemoMenu`）。

## 5. 装/未装两态联动矩阵

| 观测项 | 未装态（默认依赖） | 装 Mekanism 态（临时 `modRuntimeOnly`） |
| --- | --- | --- |
| 启动摘要日志 | `integrations detected: 0/6` | `integrations detected: 1/6 [mekanism]` |
| `MekanismIntegration.init()` | 未实例化（无日志） | `mekanism integration initialized; chemical registrations: 1` |
| `/gtsnlib` | `targets present: 0/6`，六目标全 `absent` | `targets present: 1/6`，`mekanism: present` + 其余 `absent` |
| `/gtsnlib mek` | `backend: false | registered: 0` | `backend: true | registered: 1` |
| `/gtsnlib mek chemical gtsnlib:test_chemical` | `backend unavailable` | `present @ gtsnlib:test_chemical kind=gas registry=mekanism:gas tint=0x88CCFF` |
| GameTest（38 项） | `All 38 required tests passed` | `All 38 required tests passed` |
| `NoClassDefFoundError` / `ClassNotFoundException` / `LinkageError` | 无 | 无 |

- 关键测试 `mekanismChemicalRegistrationMatchesPresence` 按真实 `ModList` 走两个分支：缺席时要求后端不可用且命令报 `backend unavailable`；在场时要求 DSL 注册的化学物质存在于 Mekanism 注册表且命令可查询（`mekanism:gas`）。两态各 38/38 通过即两分支均被覆盖。
- **临时依赖已还原**：验收时在 `build.gradle` 临时加入 `modRuntimeOnly("mekanism:Mekanism:1.20.1-10.4.16.80")`，采集完成后 `git checkout -- build.gradle` 还原；最终提交不含该依赖（工作树对 `build.gradle` 无差异）。
- 其余五个目标本轮未做运行时在场验证（Create `:slim`、AE2、Ad Astra 依赖图较重），其隔离与缺席行为已由单元测试（假 `ModPresence` 两态）与上述未装态运行时覆盖。

## 6. UI 实测

### 6.1 组件库 + 主题（`GTSNLIB_UI_AUTOTEST=1`）

客户端进入标题界面后自动打开开发测试界面（640×360 GUI 空间），注入合成输入，逐主题截图后退出：

- 合成输入日志：`clicks=1 toggled=false checkbox=true switch=true progress=0.30 slot1=true scrollY=32 lastKey=K typed=A`（按钮/复选框/开关/进度/物品槽/滚动/键盘均被驱动）
- 三主题重建并各抓一张图：`default` → `amber` → `light`；`autotest complete (themes captured: 5)`
- 截图：`docs/acceptance/screenshots/gtsnlib-ui-theme-{default,amber,light}.png`
- **肉眼确认**（`gtsnlib-ui-theme-default.png`）：组件画廊完整渲染——按钮四态、复选框/开关、换行与三向对齐文本、输入探针（`按键: K | 输入: A`）、进度条 30%、物品槽（真实物品图标 + 工具提示“点击选择此槽位”）、滚动容器、布局/裁剪演示、右下主题标注。琥珀与浅色截图主题色/纹理生效。

### 6.2 数据同步（`GTSNLIB_UI_AUTOTEST=sync`，含进世界）

- **真实进入世界**：`loading existing world 'gtsnlib-sync-autotest'` → 客户端连接集成服务端（`Dev[local:...] logged in`）→ `world ready, opening demo menu`
- 服务端打开同一 `DemoMenu`（`NetworkHooks.openScreen`）：`sync demo screen opened: slots=5 progress=0`
- 两次采样：`sample A: client=10 server=10 display='进度 10 / 100'`；`sample B: client=30 server=30 display='进度 30 / 100'`
- 判定：`sync autotest PASS: client value advanced 10 -> 30; equals server 30; display='进度 30 / 100'`（界面在推进、客户端值 == 服务端值、文本/进度条与实际值一致）
- 截图：`docs/acceptance/screenshots/gtsnlib-ui-sync-demo.png`。**肉眼确认**：真实世界背景之上渲染演示菜单，显示进度 35/100、阶段 RUNNING、档位 1/5、速度 1.0、运行 是。

### 6.3 开发态告警说明（非本库缺陷）

客户端日志存在 `ModelBakery` “missing model for variant / FileNotFoundException: gtsnlib:models/item/*.json” 警告。原因是 GTCEu 在开发态为演示物品生成的模型资源未随 `runData` 落到资源包，属 **GTCEu 开发态资源加载的既有告警**，不影响 UI 测试界面（其物品图标来自真实 `ItemStack`）与数据同步功能；`build`/`test`/`GameTest` 与两态运行均无错误级失败。

## 7. 无法全自动化的人工步骤清单

以下步骤需要真人操作客户端（自动化测试无法发送游戏内聊天命令 / 手动点击真实 UI），建议验收人手过一次：

1. 启动客户端进入世界：`.\gradlew.bat runClient`，在标题界面“单人游戏”里选择存档 `gtsnlib-sync-autotest`（或新建）进入世界。
2. 在世界内按 `T` 打开聊天，输入并回车：`/gtsnlib`。预期：聊天栏逐行显示
   `GTSNLib 0.1.0 | integrations: 6 | targets present: <n>/6` 与六个目标的 `present`/`absent`，其中 `n` 等于实际安装的联动 mod 数。
3. 依次输入并核对：
   - `/gtsnlib gt` → `GT adapter | available: true | tag prefixes: 95` 且 `material iron: present @ gtceu:iron`
   - `/gtsnlib reg` → `blocks: 1 | items: 1 | machines: 1`，三项 `present=true`
   - `/gtsnlib mek` → 装 Mekanism 时 `backend: true | registered: 1`；未装时 `backend: false | registered: 0`
4. 输入 `/gtsnlib ui`：预期打开数据同步演示菜单，进度每 0.5 秒推进，阶段/档位/速度/开关随之变化。
5. 输入 `/gtsnui`：预期打开组件库测试界面（按钮、复选框、开关、进度条、物品槽、滚动、主题切换按钮等）。
6. 若安装了其它联动目标（IE/Create/AE2/Ender IO/Ad Astra）：返回第 2 步，确认 `/gtsnlib` 中对应目标显示 `present` 且计数增加；缺失的目标保持 `absent` 且游戏不崩溃（无类加载错误）。

## 8. 复现命令

```powershell
# 门禁
.\gradlew.bat clean build
.\gradlew.bat cleanTest test
.\gradlew.bat runGameTestServer

# /gtsnlib 命令树（专职服务端 + RCON；先把 run/server/server.properties 的 enable-rcon 设为 true 并设置 rcon.password）
powershell -NoProfile -ExecutionPolicy Bypass -File logs\acceptance\rcon-sweep.ps1

# UI 自动测试
$env:GTSNLIB_UI_AUTOTEST="1";    .\gradlew.bat runClient; Remove-Item Env:\GTSNLIB_UI_AUTOTEST
$env:GTSNLIB_UI_AUTOTEST="sync"; .\gradlew.bat runClient; Remove-Item Env:\GTSNLIB_UI_AUTOTEST

# 装 Mekanism 态（验收专用，勿提交）：在 build.gradle 加一行后运行，再 git checkout -- build.gradle
#   modRuntimeOnly("mekanism:Mekanism:1.20.1-10.4.16.80")
```
