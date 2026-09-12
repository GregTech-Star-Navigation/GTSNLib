# 目标 GTCEu 7.5.3，并以适配层隔离上游 API

GTSNLib 编译目标锁定 GTCEu 1.20.1 稳定版 **7.5.3**；对 GTCEu 的注册 / 材料 / 流体访问统一经由 `com.gtsn.lib.gt.adapter` 收敛，便于上游 8.0.0 发布后迁移（8.0.0 移除了 `materialManager`、`MaterialRegistryEvent`、`registerRegistrate` 等）。

**Considered Options**: 全量直接使用 7.5.3 API（迁移面广）；目标 8.0.0-SNAPSHOT（快照不稳定，首批框架不宜）。

**Consequences**: 新增少量适配层代码；换取上游换代时的可迁移性。
