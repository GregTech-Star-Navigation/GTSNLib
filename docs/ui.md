# GTSN UI（phase 4：容器数据同步）

不依赖 LDLib 的自研 UI 框架（ADR-0004）。**phase 1**（#16）交付内核：布局引擎、渲染抽象、输入路由、控件基类、`GtsnScreen` 与开发测试界面；
**phase 2**（#17）交付组件库：面板 / 文本 / 按钮 / 进度条 / 物品槽 / 滚动容器 / 复选框 / 开关 / 工具提示 / 分隔线 / 占位；
**phase 3**（#18）交付主题 / 资源系统：语义颜色角色、文本度量、间距与圆角刻度、纹理引用、JSON 资源加载、主题注册表与切换；
**phase 4**（#19）交付容器数据同步：类型编解码的数据槽布局、服务端写 / 客户端读、变更监听与控件绑定层，含游戏内演示菜单与自动测试。

> 机器状态桥接（#22，方案 A：只读数据通道 + 自带 UI 组件）：把 GTCEu 机器状态读进本框架的组件
> （能量条 / 流体罐 / 进度箭头 / 槽位面板 / 状态面板）与 `MachineStatusScreen`，不接管 GT 的 LDLib 界面。
> 用法、只读边界与客户端同步语义见 `docs/gt-machine-bridge.md`。
>
> 自定义字体（#23）：随库打包 OFL 字体（更纱黑体 Sarasa UI SC 子集），GTSN UI 文本默认使用；
> 缺字形经 `reference` provider 回退原版字库。许可、字体 id 与覆盖方式见「自定义字体（#23）」一节。

## 包结构

| 包 | 内容 | MC 依赖 |
| --- | --- | --- |
| `com.gtsn.lib.ui.layout` | 布局引擎：盒模型（padding/margin）、尺寸策略（fixed/wrap/fill）、栈布局（方向/间距/主轴对齐/交叉轴对齐/权重）、测量约束（MeasureSpec）、九宫格锚定 | 无 |
| `com.gtsn.lib.ui.input` | 输入事件模型（鼠标/滚轮/键盘/字符）、命中测试、焦点管理、事件冒泡路由、悬停路径与鼠标位置 | 无 |
| `com.gtsn.lib.ui.render` | `RenderContext` 渲染抽象 + `TextureRef` / `SlotIcon`（纯）；`GuiGraphicsRenderContext`（原版后端）；`ItemStackIcon`（物品堆叠渲染） | 实现类客户端 |
| `com.gtsn.lib.ui.theme` | 主题模型：`Theme` / `ThemeColorRole` / `ThemeColor` / `ThemeTextStyle`（含字体 id `FontId`）/ `FontId` / `ThemeSpacing` / `ThemeRounding` / `ThemeTextureRole`、JSON 解析器、继承解析器、注册表与全局上下文 | 无 |
| `com.gtsn.lib.ui.sync` | 容器数据同步：`IntStore` / `SyncCodec` / `SyncCodecs` / `SyncSlot` / `SyncLayout` / `MenuSync` / `SyncSubscription`（纯）；`MenuSyncData` / `SyncedMenu`（原版 `AbstractContainerMenu` 数据槽适配） | 适配器引用原版公共类 |
| `com.gtsn.lib.ui.sync.bind` | 绑定层：`SyncBinding` / `AbstractSyncBinding` / `LabelBinding` / `ProgressBarBinding` / `SyncBindings` / `SyncBindingGroup` | 无 |
| `com.gtsn.lib.ui.widget` | `Widget` / `AbstractWidget` 与组件库：`Stack`、`PanelWidget`、`TextWidget`（换行/对齐/行距/字体覆盖）、`ButtonWidget`、`ProgressBarWidget`、`ItemSlotWidget`、`ScrollPanelWidget`、`CheckboxWidget`、`ToggleSwitchWidget`、`DividerWidget`、`SpacerWidget`、`Tooltip`、`TextMetrics`（字体感知度量）、`ThemedTextMetrics`（绑定生效主题字体的度量接缝，#24 F2-1）、`MachineStatusView`（MC-free 机器面板视图，#24 F1-3） | 无 |
| `com.gtsn.lib.ui.screen` | `WidgetHost`（布局/渲染/输入/工具提示覆盖层泊点，纯逻辑）；`GtsnScreen`（Screen 基类）；`GtsnUiTestScreen`（开发测试界面）；`DemoMenuScreen`（数据同步演示屏幕） | Screen 类客户端 |
| `com.gtsn.lib.ui.demo` | 开发测试界面装配：`DemoContent`（组件画廊）、`DemoState`、`DemoIcons`（客户端图标接缝）、`KeypadWidget`、`ThemeControl`（主题切换接缝）；数据同步演示：`DemoSync`（槽位与推进规则）、`DemoMenu`（演示菜单）、`DemoMenus`（菜单类型注册）、`SyncDemoContent`（演示装配，纯） | 菜单类公共侧 |
| `com.gtsn.lib.ui.client` | 客户端接线：`/gtsnui` 客户端命令与自动测试开关、`ThemeResources`（资源重载加载主题）、`GtsnUiThemeEvents`（重载事件注册）、`GtsnUiScreens`（菜单屏幕注册）、`GtsnUiSyncAutotest`（数据同步自动测试）、`ClientFonts`（字体可用性 + `Style.withFont` 套用）、`ThemeFontMetrics`（主题字体度量）、`MachineStatusViewBinder`（GT 快照 → MC-free 机器视图，#24 F1-3） | 客户端 |

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

## 主题/资源系统（#18）

**模型**（`ui.theme`，纯 Java、可无 MC 单测）：

| 组成 | 说明 |
| --- | --- |
| `ThemeColorRole` | 语义颜色角色（background / panel_* / text* / accent / success / warning / danger / disabled / border* / focus_ring / button_* / checkbox_* / switch_* / progress_* / slot_* / overlay_* / scroll_* / divider / box / input / tooltip_*，共 51 个），每个角色带内置默认值（保底视觉） |
| `ThemeColor` | 颜色引用：字面量或角色；控件字段默认持角色，渲染时经当前主题解析；字面量覆盖优先 |
| `ThemeTextStyle` | 文本度量：默认阴影、行距 |
| `ThemeSpacing` / `Spacing` | 间距刻度 xs/sm/md/lg/xl（默认 2/4/6/8/10） |
| `ThemeRounding` / `RoundingSize` | 圆角刻度（预留：当前过程化控件为直角填充） |
| `ThemeTextureRole` | 纹理角色（panel / button* / slot / scroll_* / progress_* / tooltip） |
| `Theme` | 解析视图：沿 `extends` 继承链取值，链上均未定义时回退角色内置默认值 |
| `ThemeDefinition` + `ThemeParser` | JSON 解析（结构错误显式抛 `ThemeParseException`；未知角色键 / 未知小节忽略，前向兼容） |
| `ThemeResolver` + `ThemeRegistry` | 继承解析（支持前向引用、未知父主题、`extends` 成环容错）；注册表始终含默认主题，其余按 id 字典序 |
| `ThemeContext` | 全局上下文：`setRegistry` / `setActive` / `cycle` / `active`（未知主题回退默认） |

**资源格式**（`assets/<namespace>/ui/themes/*.json`，文件名即主题 id 的路径段）：

```json
{
  "name": "浅色",
  "extends": "gtsnlib:default",
  "colors": { "panel_background": "#FFF4F6FA", "text": "#FF2A3038" },
  "text": { "shadow": false, "line_spacing": 1 },
  "spacing": { "lg": 10, "xl": 14 },
  "rounding": { "small": 2, "medium": 4, "large": 6 },
  "textures": { "panel": "gtsnlib:textures/gui/panel_light.png" }
}
```

颜色为 `#RRGGBB` / `#AARRGGBB`（`#` 可省略，6 位视为不透明）。随库分发 `default`（显式声明全部角色）、`light`（浅色 + 面板网格纹理）、`amber`（琥珀，部分覆盖演示继承）三个主题。

**回退规则**：单文件解析失败 → 跳过并记录日志（其余主题与旧注册表不受影响）；角色未定义 → 沿父链 → 最终角色内置默认值；未知主题名 → 注册表默认主题；纹理不可绘制（资源缺失 / 无资源能力的上下文）→ 纯色回退。

**渲染接入**（不破坏既有契约，全部为扩展）：`RenderContext.theme()`（默认跟随 `ThemeContext`）、`textureReady(TextureRef)`（默认 `false`）、`blitTiled(...)`（默认逐格 `blit`）；`GtsnScreen` 逐帧携带当前主题并绘制 `BACKGROUND` 角色，支持屏幕级 `setTheme` 覆盖；`WidgetHost` 工具提示与全部组件默认色改经主题角色解析，既有 `colors(...)` / `color(...)` 字面量 setter 语义不变；`PanelWidget.background(ThemeColorRole)` / `border(ThemeColorRole, int)`、`BoxWidget.fill/border(角色)`、`DividerWidget.color(角色)`、`TextWidget.colorRole(...)` 为新增角色 API——面板角色背景在纹理可用时平铺主题纹理（缺纹理回退纯色），显式字面量背景不受纹理影响。

**切换**：`ThemeContext.cycle()` / `setActive(ThemeId)`；开发测试界面右上角“主题: <名称>”按钮即切换入口，屏幕在 tick 中检测主题变化并以新主题重建控件树（`DemoState` 保留点击/开关/进度/选中槽位等状态）。

## 自定义字体（#23）

GTSN UI 文本可指定字体资源；默认主题使用随库打包的 **更纱黑体 Sarasa Gothic "Sarasa UI SC"**（面向屏幕 UI 的无衬线黑体，Latin 与 CJK 协调）。
范围**仅限经本框架渲染的文本**——不覆盖原版默认字体，不影响其它 mod / 原版 UI。

**字体资源**（字体 id `gtsnlib:sarasa_ui_sc`）：

| 文件 | 内容 |
| --- | --- |
| `assets/gtsnlib/font/sarasa_ui_sc.json` | 字体定义：`providers` = [`ttf` 子集字体, `reference` → `minecraft:default`]（顺序即优先级，首个命中者胜） |
| `assets/gtsnlib/font/sarasa_ui_sc.ttf` | Sarasa UI SC Regular 子集，**1,251,540 字节（约 1.19 MiB）**；覆盖 Latin 基础 / Latin-1 / Latin Ext-A·B、常用符号、GB2312 一级汉字与标点 |
| `assets/gtsnlib/font/license-sarasa-gothic.txt` | SIL Open Font License 1.1 全文（随 jar 打包） |

```json
{
  "providers": [
    { "type": "ttf", "file": "gtsnlib:sarasa_ui_sc.ttf", "size": 11.0, "oversample": 2.0,
      "shift": [0.0, 0.0], "skip": [" "] },
    { "type": "reference", "id": "minecraft:default" }
  ]
}
```

- **`file` 相对 `font/` 目录**：`TrueTypeGlyphProviderDefinition.load` 会执行 `location.withPrefix("font/")`，
  故写法是 `gtsnlib:sarasa_ui_sc.ttf`（**不是** `gtsnlib:font/sarasa_ui_sc.ttf`，否则解析为 `font/font/...` 加载失败）。
- **`reference` 回退层是必需的**：自定义字体不会自动回退原版；`skip: [" "]` 让空格沿用原版空格度量。
  子集未覆盖的字形（如 GB2312 二级字「龘」U+9F98）由该层用原版字库渲染。
- `size: 11.0` 匹配原版字体的视觉大小；`oversample: 2.0` 提升缩放下的清晰度；行高仍取原版 `font.lineHeight`（布局度量不因此变化）。

**主题 / 文本接线**：

- `ThemeTextStyle` 新增 `fontId`（`FontId`，如 `gtsnlib:sarasa_ui_sc`）；主题 JSON 的 `text.font` 声明，沿 `extends` 链继承；链上均未声明时回退 `FontId.VANILLA`（`minecraft:default`）。
- 渲染路径：`RenderContext` 新增字体感知重载（`text` / `textWidth` / `centeredText` 的 `FontId` 参数；`null` = 原版默认）。
`GuiGraphicsRenderContext` 把字体经 `Style.withFont(fontId)` 套用；`TextMetrics.width(text, fontId)` 用同一字体度量，保证换行 / 固有尺寸与实际渲染宽度一致。
- **度量接缝（#24 F2-1）**：客户端 `ThemeFontMetrics` 绑定**本屏生效主题**（`GtsnScreen#textMetrics()`，含 `setTheme` 覆盖）解析出的字体，经 MC-free 的 `ThemedTextMetrics` 度量——不再逐次读全局 `ThemeContext.active()`；
  屏幕覆盖主题字体时，度量 / 换行与 `GuiGraphicsRenderContext` 的渲染字体保持同一字体。单参 `ThemeFontMetrics(Font)` 保留「跟随全局主题」的默认路径。
- 逐控件覆盖：`TextWidget.font(FontId)`（指定字体）、`.vanillaFont()`（强制原版）、`.useThemeFont()`（恢复跟随主题）；覆盖同时驱动度量与渲染。
- 逐样式覆盖：任何主题可写 `"text": { "font": "gtsnlib:sarasa_ui_sc" }`；第三方字体只要放进 `assets/<ns>/font/` 即可被主题引用。
- **安全回退**：`ClientFonts.ready` 校验字体定义、`ttf` 引用文件（按 `font/` 前缀）与 sfnt magic；任何一步失败都回退原版字体（不套 `Style.withFont`），避免 `FontManager` 落到 missing 字体集渲染豆腐块。

**开发测试界面**：右列表头下方新增字体对比行——样例文本「GTSN UI 中文 English 0123 龘」默认主题字体，
按钮「字体: 主题 ↔ 字体: 原版」切换为原版字体对比（龘 即子集外字形的回退演示）。
自动测试额外抓取 `run/screenshots/gtsnlib-ui-font-theme.png` / `gtsnlib-ui-font-vanilla.png` 两张对比截图。

**验证**（无 MC 单测，headless）：

- `ThemeFontTest`：`FontId` 解析 / 哨兵、`text.font` 解析与非法值拒绝、继承与缺省回退、两参数 `ThemeTextStyle` 兼容。
- `FontResourceFilesTest`：字体定义 provider 链（ttf → reference）、`file` 按 MC 语义解析存在、子集 TTF 的 cmap 覆盖（含/缺字形）与回退选择、OFL 许可随包、默认主题指向打包字体、以及子集 TTF 保留 STB 光栅化必需表与 cmap format 4（防止丢表静默回退）。
- `TextWidgetFontTest`：字体覆盖驱动换行 / 固有尺寸 / 渲染参数，居中按字体感知宽度。

**运行期正向验证（#23 复检新增）**：

- `GtsnUiFontProbe`（客户端）以同一样例在「主题字体 / 原版字体」下的 `Font.width` 差异为正向证据：
  自动测试记录 `[GTSNLib] UI effective font = gtsnlib:sarasa_ui_sc (… themeWidth=137, vanillaWidth=140 …)`
  并断言两者必须不同。若自定义字体被安全回退静默吞掉，二者相等 → 断言抛异常，自动测试直接报错，
  无需肉眼比对截图即可捕获回归（`ClientFonts.ready` 只能校验资源存在，无法证明 MC 真用了 TTF）。
- `GtsnUiTestScreen.init()` 每次打开测试界面都会记录 `[GTSNLib] UI effective font = <id>`。
- 放大对比证据：`docs/acceptance/screenshots/font-zoom-compare-4x.png`（>=4x 最近邻裁剪，主题 vs 原版上下堆叠），
  由 `docs/acceptance/zoom-font-proof.ps1` 从两张自动测试截图生成；观测与像素差见 `docs/acceptance/font-zoom-proof.txt`。

## 容器数据同步（#19）

**模型**（`ui.sync`：纯 Java 核心 + 原版数据槽适配；不引入自定义网络包——int 数据槽覆盖 int/float（原始位）/bool/枚举/索引，足够本轮类型需求）：

| 组成 | 说明 |
| --- | --- |
| `IntStore` | 后端抽象（`get`/`set`/`size`）；单测以内存后端驱动，游戏内实现为 `MenuSyncData` |
| `SyncCodec<T>` / `SyncCodecs` | 类型 ↔ int 编解码：任意 / 区间 int（编解码钳制）、float（原始位精确往返；非有限值写侧拒绝、读侧兜底）、bool、枚举（序数 + 越界回退到首个常量或指定常量）、index（`[0, size)` 钳制）。**读侧对任意原始值兜底不抛异常**，写侧拒绝非法值 |
| `SyncSlot<T>` / `SyncLayout` | 槽位描述符与注册表：按声明顺序分配索引、名称唯一、默认值按编解码规则归一、`build()` 后冻结；槽位句柄两端共用 |
| `MenuSync` | 绑定布局与后端：服务端权威写 `set`、两端 `get`、`refresh()` 检测外部写入（客户端收包直写后端）并按槽通知 `onChange` 监听器；监听器异常隔离、订阅可取消、跨布局槽位与容量不足显式拒绝 |
| `MenuSyncData` / `SyncedMenu` | `SimpleContainerData` 的 `IntStore` 适配；菜单基类在构造期 `addDataSlots` 注册（服务端写入由原版 `broadcastChanges` 自动推送到客户端，随包到达客户端 `setData` 落回后端） |
| `ui.sync.bind` | 绑定层：`SyncBindings.progressBar/label` 把同步值接到进度条 / 文本控件；`SyncBindingGroup.refresh()` 由屏幕 tick 驱动（先 `MenuSync.refresh()` 再经监听器落实控件更新）；值未变不重复应用，`close()` 解绑 |

**数据流**：服务端写 `MenuSync.set` → `ContainerData` 后端 → `AbstractContainerMenu.broadcastChanges()` → `ClientboundContainerSetDataPacket` → 客户端 `setData` 写入后端 → 屏幕 `tick()` 调 `SyncBindingGroup.refresh()` → 监听器 → 控件更新。

**演示与入口**：`ui.demo` 的 `DemoSync`（progress/speed/active/phase/tier 五种类型槽位，含纯推进规则）+ `DemoMenu`（服务端每 10 tick 推进，进度回卷时轮换档位/速度/开关）+ `DemoMenuScreen`（进度条 + 各类型值文本）。

打开方式：**服务端命令 `/gtsnlib ui`**（经 `NetworkHooks.openScreen`；本地/服务器均可用，需玩家执行）。

## 类加载纪律

- 布局 / 输入 / 渲染接口 / 控件 / 演示装配为**纯 Java**（不引用 `net.minecraft`），专职服务端与 GameTest 可安全加载。
- `ui.screen.GtsnScreen`、`ui.screen.GtsnUiTestScreen`、`ui.render.GuiGraphicsRenderContext`、`ui.render.ItemStackIcon`、`ui.client.GtsnUiClient` 仅在客户端加载；
  `GtsnUiClient` 以 `@Mod.EventBusSubscriber(value = Dist.CLIENT)` 注册事件，专职服务端扫描时不会加载该类。
- 字体接线同样保持无 MC 依赖内核：`FontId` / `ThemeTextStyle.fontId` / `RenderContext` 字体重载 / `TextMetrics.width(text, font)` / `ThemedTextMetrics` 均为纯 Java；
  `ClientFonts` / `ThemeFontMetrics`（`ui.client`）与 `GuiGraphicsRenderContext` 仅客户端加载。
- 机器面板同守无 MC 依赖（#24 F1-3）：`MachineStatusView` / `MachineStatusPanel` / `MachineSlotsPanel` 位于 `ui.widget` 且不引用 `net.minecraft`；
  `ItemStack` / `FluidStack` → 视图模型的翻译只在 `ui.client.MachineStatusViewBinder`（测试无需 `MinecraftTestBootstrap`）。
- common / 服务端代码不得引用上述客户端类（`runServer` 日志为证）。

### 字体后续事项（#24 DEFER，仅记录）

- **F2-2**：`GuiGraphicsRenderContext` 每帧为每段文本构造 `Component.withStyle` 会产生临时分配；当前文本规模可接受（见代码 TODO），
  方案 B / 大量仪表盘文本时需按 `(text, fontId)` 缓存。
- **F2-4**：第三方字体的定义 / ttf 只做存在性、`providers` 结构、`font/` 路径解析与 sfnt magic 校验，不做深度表校验；损坏字体由 MC `FontManager` 兜底。
- **F2-5**：随包字体（Sarasa）的 OFL 许可在 `assets/gtsnlib/font/`，仓库顶层暂无第三方资源 NOTICE；后续补顶层 NOTICE 汇总。

## 开发测试界面

打开方式（二选一）：

1. **游戏内客户端命令** `/gtsnui`：经 Forge `RegisterClientCommandsEvent` 注册，本地执行、不发往服务器（远程服务器上同样可用）。
2. **自动测试**（CI / 无人值守证据）：启动客户端前设置环境变量 `GTSNLIB_UI_AUTOTEST=1`。客户端进入标题界面后：
   将窗口固定为 1280x720 + GUI 缩放 2（640x360 GUI 空间），自动打开测试界面，注入合成点击/滚轮/键盘输入并悬停物品槽（展示工具提示），
   随后点击“主题”按钮逐主题各抓一张截图（`run/screenshots/gtsnlib-ui-theme-<主题>.png`：默认/琥珀/浅色；主题经资源加载，切换后控件树按新主题重建），
   最后切回默认主题并点击“字体”按钮抓取字体对比截图（`run/screenshots/gtsnlib-ui-font-theme.png` / `gtsnlib-ui-font-vanilla.png`，#23），自动退出客户端。

> 自动测试为开发专用；正常游玩请使用 `/gtsnui`。

界面内容（组件画廊）：按钮四态与点击计数、进度条（按钮推进 / 重置 / 钳制）、复选框与开关（含禁用态）、
文本换行与三向对齐、分隔线、物品槽（真实物品图标、选择互斥、禁用槽、工具提示）、滚动容器（滚轮 + 滚动条）、
裁剪与锚定演示、键盘输入探针。

## 验证

```bash
.\gradlew.bat test               # 单测：布局几何 / 输入路由 / 各组件状态机 / 工具提示 / 演示装配 / 主题解析·继承·回退·切换 /
                                 #       数据同步编解码·槽位注册表·变更通知·绑定层·演示规则（无 MC）
.\gradlew.bat runGameTestServer  # GameTest：内核 + 组件库 + 主题 + 数据同步（默认值发布/服务端推送/客户端镜像绑定/推进回卷）在专职服务端的真实加载环境中行为验证
.\gradlew.bat runServer          # 专职服务端：客户端类不被加载（类加载纪律）
.\gradlew.bat runClient          # 端到端：$env:GTSNLIB_UI_AUTOTEST=1 自动打开 + 交互 + 逐主题截图 + 字体对比截图（#23）+ 退出
.\gradlew.bat runClient          # 数据同步端到端：$env:GTSNLIB_UI_AUTOTEST=sync 创建/载入存档、游戏内打开演示菜单、
                                 #   验证客户端值=服务端值且数值推进、抓图 run/screenshots/gtsnlib-ui-sync-demo.png、退出
.\gradlew.bat runClient          # 机器状态端到端：$env:GTSNLIB_UI_AUTOTEST=machine 创建/载入存档、放置 test_machine、
                                 #   打开机器状态界面、校验快照并抓图 run/screenshots/gtsnlib-ui-machine-status.png、退出
                                 #   （#22，见 docs/gt-machine-bridge.md）
```

上述两种自动测试的结果、截图与判定见 `docs/acceptance.md`（#20 集成验收，证据留档于 `docs/acceptance/`）。
