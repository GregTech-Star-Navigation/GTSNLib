# GTSN UI（phase 2：组件库）

不依赖 LDLib 的自研 UI 框架（ADR-0004）。**phase 1** 交付内核：布局引擎、渲染抽象、输入路由、控件基类、`GtsnScreen` 与开发测试界面；
**phase 2**（#17）在内核之上交付组件库：面板 / 文本 / 按钮 / 进度条 / 物品槽 / 滚动容器 / 复选框 / 开关 / 工具提示 / 分隔线 / 占位。
主题/资源（#18）、容器数据同步（#19）为后续轮次，不在本轮范围。

## 包结构

| 包 | 内容 | MC 依赖 |
| --- | --- | --- |
| `com.gtsn.lib.ui.layout` | 布局引擎：盒模型（padding/margin）、尺寸策略（fixed/wrap/fill）、栈布局（方向/间距/主轴对齐/交叉轴对齐/权重）、测量约束（MeasureSpec）、九宫格锚定 | 无 |
| `com.gtsn.lib.ui.input` | 输入事件模型（鼠标/滚轮/键盘/字符）、命中测试、焦点管理、事件冒泡路由、悬停路径与鼠标位置 | 无 |
| `com.gtsn.lib.ui.render` | `RenderContext` 渲染抽象 + `TextureRef` / `SlotIcon`（纯）；`GuiGraphicsRenderContext`（原版后端）；`ItemStackIcon`（物品堆叠渲染） | 实现类客户端 |
| `com.gtsn.lib.ui.widget` | `Widget` / `AbstractWidget` 与组件库：`Stack`、`PanelWidget`、`TextWidget`（换行/对齐/行距）、`ButtonWidget`、`ProgressBarWidget`、`ItemSlotWidget`、`ScrollPanelWidget`、`CheckboxWidget`、`ToggleSwitchWidget`、`DividerWidget`、`SpacerWidget`、`Tooltip`、`TextMetrics` | 无 |
| `com.gtsn.lib.ui.screen` | `WidgetHost`（布局/渲染/输入/工具提示覆盖层泊点，纯逻辑）；`GtsnScreen`（Screen 基类）；`GtsnUiTestScreen`（开发测试界面） | Screen 类客户端 |
| `com.gtsn.lib.ui.demo` | 开发测试界面装配：`DemoContent`（组件画廊）、`DemoState`、`DemoIcons`（客户端图标接缝）、`KeypadWidget` | 无 |
| `com.gtsn.lib.ui.client` | 客户端接线：`/gtsnui` 客户端命令与自动测试开关 | 客户端 |

## 组件库（#17）

| 组件 | 状态模型（可无 MC 单测） | 关键行为 |
| --- | --- | --- |
| `PanelWidget` | —（容器） | 背景 / 边框 / 标题头（内容区自动下移）；栈布局参数 |
| `TextWidget` | 换行结果 / 对齐 | `wrap(px)` 按词换行、超长单词硬断、`\n` 强制换行；`align(LEFT/CENTER/RIGHT)`；`lineSpacing(px)` |
| `ButtonWidget` | `ButtonState`：NORMAL / HOVERED / PRESSED / DISABLED | 边界内按下并释放触发；聚焦时 Enter/Space；禁用不可聚焦 |
| `ProgressBarWidget` | `min`/`max`/`value`（钳制）/`progress()` | 任意区间、比例填充、边框、渐变、`label(progress -> "..%")` |
| `ItemSlotWidget` | 悬停 / 按下 / 选中 / 禁用 + `SlotIcon` | 原版风格槽底；`selectable` 模式点击/键盘切换选择；图标绘制区为槽内缩 1px |
| `ScrollPanelWidget` | `scrollY`（钳制到 `[0, maxScroll]`）/ 滑块拖拽 | 滚轮 + 滚动条拖拽；**重排式滚动**（内容子树整体偏移），渲染裁剪与命中测试天然一致；内容按自然高度测量（不受视口 AT_MOST 钳制） |
| `CheckboxWidget` | 复选 + 悬停 / 按下 / 聚焦 / 禁用 | 点击或聚焦时 Enter/Space 切换；`onChange` 回调 |
| `ToggleSwitchWidget` | 开关 + `knobPosition()`（0/1）/ 悬停 / 按下 / 禁用 | 轨道 + 滑块视觉；与复选框一致的交互语义（共享 `AbstractToggleWidget`） |
| `Tooltip` | 悬停路径最近非空提示 | `Widget.tooltip()`；宿主 `renderTooltips` 在控件树之上绘制，鼠标跟随 + 屏幕边界钳制 |
| `DividerWidget` | — | 水平/垂直分隔线（厚度/颜色） |
| `SpacerWidget` | — | 固定尺寸或权重占位，不产生绘制 |

**契约扩展（不破坏既有 API）**：`Widget.onLayout()`（宿主在 resize 布局后自顶向下通知，滚动容器据此重排）、
`Widget.tooltip()`（默认 `null`）、`InputRouter.mouseX()/mouseY()/hoveredPath()`、`WidgetHost.activeTooltip()/tooltipBounds()/renderTooltips()`。

**布局引擎修正**：固定尺寸（`Sizing.fixed`）的容器现在把自身尺寸（扣除 padding）钉给子树内部测量约束，
使嵌套的填充/拉伸子节点按容器尺寸而非祖先约束测量（此前会导致嵌套固定容器中的子控件溢出，见 `StackLayoutTest.stackedChildrenInsideFixedContainerRespectItsWidth`）。

## 类加载纪律

- 布局 / 输入 / 渲染接口 / 控件 / 演示装配为**纯 Java**（不引用 `net.minecraft`），专职服务端与 GameTest 可安全加载。
- `ui.screen.GtsnScreen`、`ui.screen.GtsnUiTestScreen`、`ui.render.GuiGraphicsRenderContext`、`ui.render.ItemStackIcon`、`ui.client.GtsnUiClient` 仅在客户端加载；
  `GtsnUiClient` 以 `@Mod.EventBusSubscriber(value = Dist.CLIENT)` 注册事件，专职服务端扫描时不会加载该类。
- common / 服务端代码不得引用上述客户端类（`runServer` 日志为证）。

## 开发测试界面

打开方式（二选一）：

1. **游戏内客户端命令** `/gtsnui`：经 Forge `RegisterClientCommandsEvent` 注册，本地执行、不发往服务器（远程服务器上同样可用）。
2. **自动测试**（CI / 无人值守证据）：启动客户端前设置环境变量 `GTSNLIB_UI_AUTOTEST=1`。客户端进入标题界面后：
   将窗口固定为 1280x720 + GUI 缩放 2（640x360 GUI 空间），自动打开测试界面，注入合成点击/滚轮/键盘输入并悬停物品槽（展示工具提示），
   抓取截图到 `run/screenshots/gtsnlib-ui-autotest.png`，随后自动退出客户端。

> 自动测试为开发专用；正常游玩请使用 `/gtsnui`。

界面内容（组件画廊）：按钮四态与点击计数、进度条（按钮推进 / 重置 / 钳制）、复选框与开关（含禁用态）、
文本换行与三向对齐、分隔线、物品槽（真实物品图标、选择互斥、禁用槽、工具提示）、滚动容器（滚轮 + 滚动条）、
裁剪与锚定演示、键盘输入探针。

## 验证

```bash
.\gradlew.bat test               # 单测：布局几何 / 输入路由 / 各组件状态机 / 工具提示 / 演示装配（无 MC）
.\gradlew.bat runGameTestServer  # GameTest：内核 + 组件库在专职服务端的真实加载环境中行为验证
.\gradlew.bat runServer          # 专职服务端：客户端类不被加载（类加载纪律）
.\gradlew.bat runClient          # 端到端：$env:GTSNLIB_UI_AUTOTEST=1 自动打开 + 交互 + 截图 + 退出
```
