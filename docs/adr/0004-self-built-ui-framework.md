# 自研 UI 框架，不基于 LDLib

GTSNLib 提供完全自研的 UI 框架（布局 / 组件 / 输入 / 主题 / 数据同步），对外 API 不绑定 LDLib。代价是无法直接复用 GTCEu 基于 LDLib 的机器界面，需另做桥接层或并存两套 UI；工程量大，分期交付但均为最终质量实现（非 MVP）。

**Status**: accepted

**Considered Options**: 基于 LDLib 封装（开发快、与 GT 生态一致，但受第三方 API 约束、外观受限于 LDLib 能力）。

**Consequences**: 显著更高的工程量与维护成本；换取完全自主、可跨 mod 复用的 UI 技术资产。
