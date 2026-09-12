# GTSNLib — 项目规则

GTSNLib 是 **GTSN（GregTech Star Navigation）** 项目群的前置库 mod（Minecraft 1.20.1 Forge）。它为基于格雷科技现代版（GTCEu）的附属 mod 提供框架：联动注册、注册简化层、自研 UI。不含具体游戏内容。

- 全局偏好见 `~/.config/opencode/AGENTS.md`
- 领域词汇见 `CONTEXT.md`；架构决策见 `docs/adr/`

## Agent skills

### Issue tracker

GitHub Issues（`gh` CLI），仓库 `GregTech-Star-Navigation/GTSNLib`。见 `docs/agents/issue-tracker.md`。

### Triage labels

五角色默认标签：`needs-triage` / `needs-info` / `ready-for-agent` / `ready-for-human` / `wontfix`。见 `docs/agents/triage-labels.md`。

### Domain docs

单上下文：根 `CONTEXT.md` + `docs/adr/`。见 `docs/agents/domain.md`。

## 常用命令

> Windows（PowerShell）下使用 `.\gradlew.bat`；其它平台用 `./gradlew`。
> 端到端验收结论与证据见 `docs/acceptance.md`。

```bash
.\gradlew.bat build               # 编译 + 单元测试 + 产出 reobf jar（build/libs/gtsnlib-<version>.jar）
.\gradlew.bat test                # 仅跑 JUnit5 单元测试（src/test/java；当前 402 项）
.\gradlew.bat runClient           # 启动开发态客户端（主菜单）
.\gradlew.bat runServer           # 启动开发态服务端（--nogui；日志 run/server/logs/latest.log）
.\gradlew.bat runData             # 数据生成，输出到 src/generated/resources/
.\gradlew.bat runGameTestServer   # 运行 GameTest（需 forge.enabledGameTestNamespaces=gtsnlib；当前 38 项）
.\gradlew.bat publishToMavenLocal # 发布 reobf 变体到 ~/.m2（供附属 mod 以 modImplementation 消费）
```

### `/gtsnlib` 命令树（诊断 + 入口）

```text
/gtsnlib                                  # 库版本 + 已登记联动数 + 六目标 present/absent
/gtsnlib gt                               # GTCEu 适配层探针（可用性、tag prefix 数、iron 探针）
/gtsnlib gt material <id>                 # 材料存在性 + 经注册简化层登记的衍生件/矿词/实时生成状态
/gtsnlib gt fluid <id>                    # 流体存在性/物态 + 材料关联（含一次性流体）
/gtsnlib reg                              # 经通用注册层登记的方块/物品/机器及真实注册表存在性
/gtsnlib mek                              # Mekanism 化学后端可用性 + 已登记数量
/gtsnlib mek chemical <id>                # 化学物质存在性/种类/颜色（Mekanism 缺席时报 backend unavailable）
/gtsnlib ui                               # （需玩家）打开容器数据同步演示菜单
```

- 客户端命令 `/gtsnui`：打开组件库开发测试界面（本地执行、不发往服务器）。
- 客户端命令 `/gtsnui machine`：对玩家注视的 GT 机器打开只读机器状态界面（#22；详见 `docs/gt-machine-bridge.md`）。
- UI 自动测试（开发专用，无人值守证据，详见 `docs/ui.md`）：
  - `$env:GTSNLIB_UI_AUTOTEST="1"`：自动打开测试界面 + 合成交互 + 逐主题截图。
  - `$env:GTSNLIB_UI_AUTOTEST="sync"`：创建/载入存档进入世界 + 打开数据同步演示 + 采样校验 + 截图。
  - `$env:GTSNLIB_UI_AUTOTEST="machine"`：创建/载入存档 + 放置 `test_machine` + 打开机器状态界面 + 校验快照 + 截图（#22）。
- 开发态 jar 在 `build/devlibs/`，可分发的 reobf jar 在 `build/libs/`。
- Mixin refmap 生成于 `build/mixin/mixins.gtsnlib.refmap.json`；开发运行的 `--mixin.config` 由 `mixinConfigJar` 任务打包后加入 run classpath。
- `gradle.properties` 固定 JDK 17（`org.gradle.java.home`），Gradle wrapper 为 8.14。
