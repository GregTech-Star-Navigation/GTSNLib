# 目标 GTCEu 7.5.4-patch01（组织 fork），并以适配层隔离上游 API

GTSNLib 编译目标锁定**组织自维护的 GTCEu fork**（`GregTech-Star-Navigation/GregTech-Modern`）版本 **`7.5.4-patch01`**，经组织 GitHub Packages 分发：

- 仓库：`https://maven.pkg.github.com/GregTech-Star-Navigation/GregTech-Modern`
- 坐标：`com.gregtechceu.gtceu:gtceu-1.20.1:7.5.4-patch01:slim`（`transitive = false`）
- **读取需要凭据**（`read:packages`）——GitHub Packages 的 Maven 不支持匿名读取

对 GTCEu 的注册 / 材料 / 流体访问统一经由 `com.gtsn.lib.gt.adapter` 收敛，便于上游 8.0.0 发布后迁移（8.0.0 移除了 `materialManager`、`MaterialRegistryEvent`、`registerRegistrate` 等）。

**Status**: accepted（2026-09-13 更新：目标由官方 `7.5.3` 改为组织 fork 的 `7.5.4-patch01`；`mods.toml` 要求区间 `[7.5.4-patch01,8.0.0)`；文件名保留历史 slug）

**Considered Options**: 官方 GTCEu `7.5.3`（原方案 —— 无组织补丁）；目标 `8.0.0-SNAPSHOT`（快照不稳定，首批框架不宜）。

**Consequences**: 构建与 CI 解析 GTM 需要 `read:packages` 凭据（`GITHUB_ACTOR`/`GITHUB_TOKEN` 或 `-Pgpr.user/-Pgpr.key`）；下游 mod 应同样使用组织 fork 的该版本；适配层继续承担上游换代的可迁移性。
