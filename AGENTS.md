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

> 脚手架落地后补充（`gradlew build` / `runClient` / `runGameTestServer` / `runData` / `publishToMavenLocal` / `test`）。
