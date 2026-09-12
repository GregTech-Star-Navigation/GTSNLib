# GTSN（GregTech Star Navigation）项目群

GTSN = **GregTech Star Navigation**（GitHub 组织：GregTech-Star-Navigation，已有兄弟 mod：GTSN-Terrain-Generation）。围绕《我的世界》格雷科技现代版（GTCEu，1.20.1 Forge）的附属 mod 体系。GTSNLib 是体系内所有自研 mod 共用的前置库；GTSNCore 等未来 mod 依赖它，分仓独立演进。

## Language

**GTSN（GregTech Star Navigation）**:
本项目的品牌与 GitHub 组织名；自研 mod 统一命名 `gtsn*`（ModID 前缀 `gtsn`，包根 `com.gtsn.<模块>`）。

**GTSNLib**:
自研 mod 体系共用的前置库 mod（ModID `gtsnlib`），承载跨 mod 联动的框架与公共设施，不实现具体游戏内容。
_Avoid_: 公共库、工具 mod、核心模组

**GTSNCore**:
规划中的主 mod，将依赖 GTSNLib；当前仅为占位目录，未启动。
_Avoid_: 核心库、主库

**附属 mod（Addon）**:
依赖 GTSNLib 的自研内容 mod 的统称（如未来的 GTSNCore），各自独立仓库、独立发布。
_Avoid_: 子模组、插件

**注册简化层（Registration Layer）**:
GTSNLib 对 GTCEu 材料 / 流体与 Mekanism 化学物质等注册 API 的声明式封装，用更短的代码完成注册及衍生件、矿词（tag）生成。

**命名空间材料注册表（Namespace Material Registry）**:
GTCEu 为每个 ModID 维护的材料注册表，附带其绑定的 `GTRegistrate`。附属 mod 的注册简化层 helper（方块/物品/机器/流体/材料）均要求其命名空间已存在该注册表——即使不注册材料，也须在 `MaterialRegistryEvent` 期间经 `GtAdapter.createRegistrate(modId)` 建立。详见 `docs/registration.md`。

**GTSN UI**:
GTSNLib 自研的、不依赖 LDLib 的界面框架（布局 / 组件 / 输入 / 主题 / 数据同步）。

**联动（Integration）**:
GTSNLib 与外部 mod（通用机械、沉浸工程、机械动力、应用能源 2、末影接口、Ad Astra〔1.20.1 上代替官方星系〕）之间的可选协作：目标 mod 在场时启用，缺席时框架保持完整可用。
_Avoid_: 兼容、适配、支持
