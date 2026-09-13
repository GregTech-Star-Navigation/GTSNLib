# GTSNLib 约束清单

> 本文件汇总 GTSNLib 的**硬性约束**（平台/依赖/架构/内容/发布/工程）。
> 与本文冲突的改动：先开 issue 说明 → 涉及架构则落 ADR → 更新本文 → 再实施。
>
> 相关：`CONTEXT.md`（词汇）· `docs/adr/`（决策）· `README.md`（使用）· `AGENTS.md`（命令）

---

## 1. 平台与工具链

| 项           | 约束                                                                                                                                                       |
| ------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Minecraft    | **1.20.1**（不迁 1.21、不迁 NeoForge）                                                                                                                     |
| Forge        | **47.4.0**（mods.toml 硬依赖 `versionRange="[47,)"`）                                                                                                      |
| Java         | **17**（toolchain；`gradle.properties` 的 `org.gradle.java.home` 指向本机 JDK17）                                                                          |
| Gradle       | **8.14**（wrapper 入库；升级需先验证 ModDevGradle 兼容性）                                                                                                 |
| ModDevGradle | `net.neoforged.moddev.legacyforge` **2.0.86**                                                                                                              |
| Mappings     | Parchment **2023.09.03**                                                                                                                                   |
| 构建仓库     | 额外 maven **必须**声明在**项目级** `build.gradle repositories`——ModDevGradle 声明了项目仓库，会覆盖 `settings.gradle` 的 `dependencyResolutionManagement` |

## 2. 依赖约束

- **GTCEu 硬依赖（组织 fork）**：`com.gregtechceu.gtceu:gtceu-1.20.1:7.5.4-patch01:slim`（`transitive = false`），来源为组织 GitHub Packages `https://maven.pkg.github.com/GregTech-Star-Navigation/GregTech-Modern`（**Maven 读取也需 `read:packages` 凭据**）；mods.toml `versionRange="[7.5.4-patch01,8.0.0)"`（**8.0 移除 `materialManager`/`MaterialRegistryEvent`/`registerRegistrate`**）
- 配套硬依赖 `ldlib` / `configuration`：mods.toml 使用**版本区间**——**空串 `""` 会被 Forge 判定为不满足而拒载**
- **Registrate 不是 mod**（其 jar 无 `mods.toml`）→ 仅作库依赖，**不得**写入 mods.toml 依赖
- **软依赖（6 个联动目标）**：`mandatory=false` + `versionRange="[0,)"`（**空串会拒载**）+ `ordering="AFTER"`；dev 侧 `modCompileOnly { transitive = false }`，**不得**进入运行时
- 任一/全部联动目标缺席时，库必须完全可用（**零 `NoClassDefFoundError`**）

## 3. 架构约束

- **ADR-0003 类加载纪律**：门面/事件订阅类**禁止**出现任何可选 mod 类型（字段/方法签名/父类/注解值）；联动模块经 `Supplier<Supplier<T>>` 延迟实例化；`ModPresence` 可注入以便 headless 单测
- **ADR-0004 自研 UI**：UI 框架**不得**依赖 LDLib；**不接管** GT 现有机器界面（#25 wontfix）——共存而非替换
- **ADR-0005 GT 隔离**：`import com.gregtechceu` **仅允许**出现在 `com.gtsn.lib.gt.adapter`（其余包越界计数必须为 0；可用搜索自证）
- **客户端/服务端分离**：客户端类**不得**在专用服务端加载（`runServer` 日志中客户端类引用必须为 0）
- **`ui.widget` 无 MC 依赖**：纯 Java、可 headless 单测；`ItemStack`/`FluidStack` 等 MC 类型只允许出现在客户端 binder
- **字体**：自定义字体仅作用于 GTSN UI 文本，**不覆盖全局默认字体**；缺字形必须回退原版字体链（`reference` provider）
- **同步证据**：涉及客户端同步的断言必须**可失败**（禁止恒真断言）；同步类改动需给出运行时真实跃迁证据

## 4. 内容约束

- GTSNLib **不含具体游戏内容**（不注册正式机器/物品/方块/配方）
- **演示内容受门控**：`DemoContentRuntime.enabled()` = `!production || config.registerDemoContent`；**生产安装默认零演示内容**
- 新增演示内容必须走同一门控，且 dev 测试保持全绿

## 5. 发布与消费约束

- **构建期需凭据**：解析组织版 GTCEu（`7.5.4-patch01`）需要 `GITHUB_ACTOR`/`GITHUB_TOKEN`（`read:packages`）或 `-Pgpr.user/-Pgpr.key`；缺失时 `build.gradle` 会在配置期输出警示日志、解析随后失败
- **版本不可覆盖**：GitHub Packages 同版本重复发布 → `409 Conflict`；重发**必须先提升** `mod_version`（`gradle.properties`）
- **消费端读取需凭据**（`read:packages`）——GitHub Packages 机制使然
- **凭据仅走环境变量**（`GITHUB_ACTOR`/`GITHUB_TOKEN`）或 `-Pgpr.user/-Pgpr.key`；**严禁**写入仓库
- 发布物必须是 **reobf 变体**（`from components.java`，**不得**改成 `artifact jar`），并含 sources jar
- 本地联调：`publishToMavenLocal`；工程布局为**独立仓库 + mavenLocal**（ADR-0001）、**独立前置 mod**分发（ADR-0002）
- License：**LGPL-3.0**（ADR-0006）；随库字体为 OFL，许可文本必须随包分发

## 6. 工程与协作约束

- **提交署名仅 `sanjiu2024`**；**禁止**任何 co-author / 署名 trailer
- 提交信息：**中文主题 + `(#issue)`**；**原子提交**（一块一提交，验证通过才提交）
- **证据要求**：任何"完成"声明必须有真实证据（`gradlew build` / `test` / `runGameTestServer` / `runServer` / `runClient` 自动测试的输出或截图）；**无证据不得声称完成**
- **TDD**：行为改动先写失败测试（RED→GREEN 留档）
- 禁止类型压制（`as any` 类）、禁止空 catch、**禁止删除测试来"通过"**
- UI/视觉改动需**放大截图目视核验**（≥2x；字体类 ≥3–4x，避免误判）
- 长任务**小步提交**（本机有断电史，避免未提交工作丢失）

## 7. 已知不做 / 限制

- **不接管** GT 机器界面渲染（#25 wontfix）。若将来重启，须按评审约束：角色感知快照（直读 `MetaMachine` traits）、字段级增量更新（禁整树重建）、主题字体穿透、避免每帧分配、保留 `of(MetaMachine)` 接缝
- UI **交互**（槽位/按钮/RPC/覆盖配置/JEI 联动）未实现——当前为**只读**展示
- 无全局字体替换；无 CurseForge/Modrinth 发布（仅 GitHub Packages + mavenLocal）
- 联动目标仅限于 6 个（Mekanism · 沉浸工程 · 机械动力 · AE2 · Ender IO · Ad Astra / 星系替代）
