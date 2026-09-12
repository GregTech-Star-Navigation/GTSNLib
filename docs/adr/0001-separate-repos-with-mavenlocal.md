# 每个 mod 独立仓库，经 mavenLocal 消费前置库

GTSNLib 与未来的各个 mod（GTSNCore 等）各自是独立的 git 仓库与独立 Gradle 构建；依赖关系通过 `publishToMavenLocal` 发布 + 消费者 `mavenLocal()` + `modImplementation(...)` 建立。

**Considered Options**: monorepo + Gradle 多模块（构建耦合、子模块难以独立发布）；composite `includeBuild`（对 reobf 库的映射支持更绕）。

**Consequences**: 模块可独立拆分、升级、重组（符合项目模块化原则）；代价是跨模块联调需先发布库，且需留意 `mavenLocal` 对 Gradle Module Metadata 的局限（备选：本地 file maven 仓库）。
