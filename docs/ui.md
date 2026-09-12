# GTSN UI 内核（phase 1）

不依赖 LDLib 的自研 UI 框架内核（ADR-0004）。本轮交付：布局引擎、渲染抽象、输入路由、控件基类、Screen 基类与内置开发测试界面。
组件库（#17）、主题/资源（#18）、容器数据同步（#19）为后续轮次，不在本轮范围。

## 包结构

| 包 | 内容 | MC 依赖 |
| --- | --- | --- |
| `com.gtsn.lib.ui.layout` | 布局引擎：盒模型（padding/margin）、尺寸策略（fixed/wrap/fill）、栈布局（方向/间距/主轴对齐/交叉轴对齐/权重）、测量约束（MeasureSpec）、九宫格锚定 | 无 |
| `com.gtsn.lib.ui.input` | 输入事件模型（鼠标/滚轮/键盘/字符）、命中测试、焦点管理、事件冒泡路由 | 无 |
| `com.gtsn.lib.ui.render` | `RenderContext` 渲染抽象 + `TextureRef`（纯）；`GuiGraphicsRenderContext`（原版后端） | 实现类客户端 |
| `com.gtsn.lib.ui.widget` | `Widget` / `AbstractWidget` 与基础控件：`Stack`、`BoxWidget`、`TextWidget`、`ButtonWidget`、`ClipWidget`、`TextMetrics` | 无 |
| `com.gtsn.lib.ui.screen` | `WidgetHost`（布局/渲染/输入泊点，纯逻辑）；`GtsnScreen`（Screen 基类）；`GtsnUiTestScreen`（开发测试界面） | Screen 类客户端 |
| `com.gtsn.lib.ui.demo` | 开发测试界面装配：`DemoContent`、`DemoState`、`KeypadWidget`（客户端与 GameTest 共用同一份装配） | 无 |
| `com.gtsn.lib.ui.client` | 客户端接线：`/gtsnui` 客户端命令与自动测试开关 | 客户端 |

## 类加载纪律

- 布局 / 输入 / 渲染接口 / 控件 / 演示装配为**纯 Java**（不引用 `net.minecraft`），专职服务端与 GameTest 可安全加载。
- `ui.screen.GtsnScreen`、`ui.screen.GtsnUiTestScreen`、`ui.render.GuiGraphicsRenderContext`、`ui.client.GtsnUiClient` 仅在客户端加载；
  `GtsnUiClient` 以 `@Mod.EventBusSubscriber(value = Dist.CLIENT)` 注册事件，专职服务端扫描时不会加载该类。
- common / 服务端代码不得引用上述客户端类（`runServer` 日志为证）。

## 开发测试界面

打开方式（二选一）：

1. **游戏内客户端命令** `/gtsnui`：经 Forge `RegisterClientCommandsEvent` 注册，本地执行、不发往服务器（远程服务器上同样可用）。
2. **自动测试**（CI / 无人值守证据）：启动客户端前设置环境变量 `GTSNLIB_UI_AUTOTEST=1`。客户端进入标题界面后自动打开测试界面，
   注入合成点击/键盘输入，抓取截图到 `run/screenshots/gtsnlib-ui-autotest.png`，随后自动退出客户端。

> 自动测试为开发专用；正常游玩请使用 `/gtsnui`。

界面内容：布局演示（栈/间距/权重/边框）、按钮点击计数、键盘聚焦输入探针（最近按键 + 字符输入）、裁剪（scissor）溢出演示、
右下角锚定徽标。

## 验证

```bash
.\gradlew.bat test               # 单测：布局几何 / 输入路由 / 控件渲染 / 演示装配（无 MC）
.\gradlew.bat runGameTestServer  # GameTest：演示树布局/点击/键盘/Tab 焦点/MC-free 渲染计数
.\gradlew.bat runServer          # 专职服务端：客户端类不被加载（类加载纪律）
.\gradlew.bat runClient          # 端到端：$env:GTSNLIB_UI_AUTOTEST=1 自动打开 + 截图 + 退出
```
