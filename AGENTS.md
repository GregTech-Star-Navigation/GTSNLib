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

```bash
.\gradlew.bat build               # 编译 + 单元测试 + 产出 reobf jar（build/libs/gtsnlib-<version>.jar）
.\gradlew.bat test                # 仅跑 JUnit5 单元测试（src/test/java）
.\gradlew.bat runClient           # 启动开发态客户端（主菜单）
.\gradlew.bat runServer           # 启动开发态服务端（--nogui；日志 run/server/logs/latest.log）
.\gradlew.bat runData             # 数据生成，输出到 src/generated/resources/
.\gradlew.bat runGameTestServer   # 运行 GameTest（需 forge.enabledGameTestNamespaces=gtsnlib）
.\gradlew.bat publishToMavenLocal # 发布 reobf 变体到 ~/.m2（供附属 mod 以 modImplementation 消费）
```

- 开发态 jar 在 `build/devlibs/`，可分发的 reobf jar 在 `build/libs/`。
- Mixin refmap 生成于 `build/mixin/mixins.gtsnlib.refmap.json`；开发运行的 `--mixin.config` 由 `mixinConfigJar` 任务打包后加入 run classpath。
- `gradle.properties` 固定 JDK 17（`org.gradle.java.home`），Gradle wrapper 为 8.14。
