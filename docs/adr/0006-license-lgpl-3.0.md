# 采用 LGPL-3.0 许可

GTSNLib 采用 **LGPL-3.0**（而非组织现有 mod GTSN-Terrain-Generation 的 MIT），因为库将大量封装、并可能直接改写 GTCEu（本身 LGPL-3.0）的注册代码，采用同族许可可避免许可摩擦；组织内容许混合许可。

**Consequences**: 若下游复制 GTSNLib 代码，需以 LGPL 兼容方式发布。
