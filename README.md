# GTSNLib

**GTSN（GregTech Star Navigation）项目群的前置库 mod** —— 为格雷科技现代版（GTCEu，Minecraft 1.20.1 Forge）的附属 mod 提供框架：

- **联动注册**：六个目标 mod（通用机械 / 沉浸工程 / 机械动力 / 应用能源 2 / 末影接口 / Ad Astra）的存在性检测与安全加载（缺席不崩）
- **注册简化层**：GT 材料（衍生件 + 矿词）、流体 / 气体 / 等离子体、Mekanism 化学物质、通用方块 / 物品 / 机器注册
- **自研 UI 框架**（不依赖 LDLib）：布局内核、组件库、主题与自定义字体（Sarasa UI SC）、容器数据同步
- **GT 机器只读桥接**：把 GT 机器状态（能量 / 进度 / 槽位 / 流体）读进自研 UI 组件

> GT 自身的机器界面保持原渲染（不做接管）。库不含具体游戏内容。

- 领域词汇：`CONTEXT.md` ｜ 架构决策：`docs/adr/`
- **约束清单：`docs/constraints.md`**（依赖 / 架构 / 发布 / 工程硬约束）
- 开发文档：`docs/ui.md` · `docs/registration.md` · `docs/gt-machine-bridge.md` · `docs/acceptance.md`
- License：**LGPL-3.0**

---

## 依赖方式

### 1) GitHub Packages（组织内分发，推荐）

> ⚠️ GitHub Packages 的 Maven registry **读取也需要凭据**（一个具备 `read:packages` 的 token）。

```groovy
// settings.gradle 或 build.gradle 的 repositories
maven {
    url = uri("https://maven.pkg.github.com/GregTech-Star-Navigation/GTSNLib")
    credentials {
        username = System.getenv("GITHUB_ACTOR")   // 或 gradle.properties 中的 gpr.user
        password = System.getenv("GITHUB_TOKEN")   // 需 read:packages 权限
    }
}
```

```groovy
dependencies {
    modImplementation("com.gtsn.lib:gtsnlib:0.1.1")   // ModDevGradle legacyforge
    // 使用 ForgeGradle 6 的项目： implementation fg.deobf("com.gtsn.lib:gtsnlib:0.1.1")
}
```

### 2) 本地消费（mavenLocal，开发联调）

```bash
./gradlew publishToMavenLocal        # Windows: .\gradlew.bat publishToMavenLocal
```

```groovy
repositories { mavenLocal() }
dependencies { modImplementation("com.gtsn.lib:gtsnlib:0.1.1") }
```

> 提示：GTSNLib 是**独立前置 mod**，玩家侧需与依赖它的 mod 一同安装（`mods.toml` 已声明软/硬依赖关系）。

---

## 发布

- 当前版本：`0.1.1`（框架期）
- **自动**：发布 GitHub Release（或手动触发 workflow）→ `.github/workflows/publish.yml` 自动构建并发布到 GitHub Packages
- **手动**：

  ```bash
  # PowerShell
  $env:GITHUB_ACTOR="<你的 GitHub 用户名>"; $env:GITHUB_TOKEN="<具备 write:packages 的 token>"
  .\gradlew.bat publish
  ```

  凭据仅从环境变量或 `-Pgpr.user/-Pgpr.key` 读取，**不会写入仓库**。

> ⚠️ **版本不可覆盖**：GitHub Packages 的 Maven 版本一旦发布即不可变，重复发布同一版本会返回 `409 Conflict`。需要重发时请先提升 `mod_version`（`gradle.properties`）。
> ⚠️ 消费端读取同样需要凭据（`read:packages`）。

## 构建（开发者）

见 `AGENTS.md`「常用命令」（`build` / `test` / `runClient` / `runServer` / `runData` / `runGameTestServer` / `publishToMavenLocal`）。
