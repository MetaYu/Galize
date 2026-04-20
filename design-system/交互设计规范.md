# Galize UI/交互设计规范

> **适用范围：** Galize Android 应用全局 UI/交互标准
> **技术栈：** Kotlin · Jetpack Compose · Material 3 · Hilt
> **设计风格：** Cyberpunk Galgame（赛博朋克视觉小说）
> **最后更新：** 2026-04-21

---

## 1. 设计理念

**"万物皆可 Galgame"** — 将社交对话变为视觉小说体验。

### 核心设计原则
| 原则 | 说明 |
|------|------|
| **沉浸感** | 深色赛博朋克视觉，霓虹色调营造游戏氛围 |
| **轻量化** | 悬浮球 + 弹出面板为主交互，不打断用户聊天流程 |
| **即时反馈** | 所有操作 ≤300ms 内给予视觉反馈 |
| **游戏化** | 三选项配色、好感度动态条、潜台词解码强化 Galgame 体感 |

---

## 2. 色彩体系

### 2.1 主色板（Dark-Only）

| 角色 | 色值 | Compose 变量 | 用途 |
|------|------|-------------|------|
| **主色** | `#7B2FFF` | `CyberPurple` | 品牌标识、悬浮球、主按钮、进度条（中段） |
| **主色-亮** | `#B388FF` | `CyberPurpleLight` | 辅助强调、tertiary |
| **强调色** | `#FF4081` | `CyberPink` | CTA、secondary accent |
| **背景色** | `#1A1025` | `CyberDark` | App 全局背景、overlay 面板底色 |
| **表面色** | `#2D1B47` | `CyberSurface` | 卡片、输入框、elevated surface |
| **前景文字** | `#FFFFFF` | `Color.White` | 主要文字 |
| **次要文字** | `#FFFFFF` α=0.6~0.7 | — | 副标题、说明文字 |
| **禁用/弱文字** | `#808080` / `Color.Gray` | — | 潜台词、dismiss 提示 |

### 2.2 三选项色（Choice Colors）

| 选项 | 色值 | Compose 变量 | 语义 |
|------|------|-------------|------|
| **Pure Heart** 💚 | `#4CAF50` | `PureHeartGreen` | 稳健/增加好感度 |
| **Chaos** 🔴 | `#FF1744` | `ChaosRed` | 攻击性/梗/终结话题 |
| **Philosopher** 🟣 | `#9C27B0` | `PhilosopherPurple` | 跳脱/意外回答 |

### 2.3 好感度色域映射

| 好感度区间 | 显示颜色 | 情绪含义 |
|-----------|---------|---------|
| 71–100 | `PureHeartGreen` | 关系良好 |
| 31–70 | `CyberPurple` | 中性/观望 |
| 0–30 | `ChaosRed` | 关系紧张 |

### 2.4 配色规则
- **仅支持深色模式**（Dark-Only），不实现 Light 主题
- 所有 UI 颜色使用 `GalizeTheme` 中定义的 Compose 变量，禁止硬编码随意色值
- 半透明背景统一使用 `CyberDark.copy(alpha = 0.95f)` 作为 overlay 底色
- 选项按钮背景使用对应 Choice 色 + `alpha = 0.15f`

---

## 3. 字体排版

### 3.1 字体选择

| 用途 | 字体 | 说明 |
|------|------|------|
| **标题** | 系统默认 (Sans Serif) | Material 3 Typography，保持 Android 原生体验 |
| **正文** | 系统默认 (Sans Serif) | 保证中文渲染质量 |
| **品牌标识** | 可选引入 Russo One / Chakra Petch | 仅用于 Logo「G」或品牌 slogan |

> **说明：** MVP 阶段使用系统字体以减少 APK 体积。后续可在品牌强化阶段引入 Google Fonts。

### 3.2 字号规范

| 层级 | 字号 | FontWeight | 场景 |
|------|------|-----------|------|
| **H1 品牌标题** | 24.sp | Bold | "万物皆可 Galgame" |
| **H2 页面标题** | TopAppBar 默认 | — | Scaffold topBar |
| **Body 正文** | 14.sp | Normal | 选项文本、状态描述 |
| **Caption 说明** | 12.sp | Normal / Italic | 潜台词、辅助说明 |
| **Overline 标签** | 10~11.sp | Bold / Medium | 选项类型标签 `[Pure Heart]`、Affinity 标签 |
| **Button 按钮** | 18.sp | Medium | 主操作按钮 |
| **Hint 弱提示** | 10.sp | Normal | "Tap outside to dismiss" |

---

## 4. 间距与布局

### 4.1 间距 Token

| Token | 值 | 用途 |
|-------|------|------|
| `xs` | 4.dp | 文字与图标间距、紧凑元素 |
| `sm` | 8.dp | 卡片内间距、元素小间距 |
| `md` | 12.dp | 选项按钮内 padding、列表间距 |
| `lg` | 16.dp | 卡片外 padding、区域分隔 |
| `xl` | 24.dp | 页面级 padding、大段落分隔 |
| `2xl` | 48.dp | 品牌标题与操作区间距 |

### 4.2 布局原则
- 页面内容使用 `padding(24.dp)` 整体内边距
- 卡片内部使用 `padding(16.dp)` + `Arrangement.spacedBy(12.dp)`
- Overlay 面板使用 `padding(8.dp)` 外边距 + `padding(16.dp)` 内边距
- `Scaffold` + `TopAppBar` 标准页面结构
- 列表页使用 `LazyColumn`，确保长列表性能

### 4.3 圆角系统

| 组件 | 圆角 | 说明 |
|------|------|------|
| **主面板/Card** | 16.dp | ChoicePanel 外层卡片 |
| **选项按钮** | 12.dp | 选项可点击区域 |
| **进度条** | 3.dp | AffinityBar 进度条 |
| **悬浮球** | CircleShape | 完全圆形 |
| **输入框** | Material 3 默认 | OutlinedTextField |

---

## 5. 组件规范

### 5.1 悬浮球 (Floating Bubble)

| 属性 | 值 |
|------|------|
| 尺寸 | 56.dp × 56.dp |
| 形状 | CircleShape |
| 背景 | `CyberPurple` α=0.9 |
| 内容 | 文字「G」, 24.sp, Bold, White |
| 交互 | 支持拖拽 + 单击 |
| 层级 | WindowManager overlay, 最顶层 |

### 5.2 选项面板 (Choice Panel)

```
┌──────────────────────────────────┐  Card: CyberDark α=0.95
│  Affinity    ████████░░  72/100  │  AffinityBar
│                                  │
│  "她可能在试探你的底线..."         │  潜台词: Gray, 12sp, Italic
│                                  │
│  ┌ [Pure Heart] ─────────────┐   │  Green α=0.15 背景
│  │  温柔回复文本               │   │  14sp White
│  │  选项描述                   │   │  11sp Gray
│  └────────────────────────────┘   │
│  ┌ [Chaos] ──────────────────┐   │  Red α=0.15 背景
│  │  攻击性回复文本             │   │
│  └────────────────────────────┘   │
│  ┌ [Philosopher] ────────────┐   │  Purple α=0.15 背景
│  │  哲学回复文本               │   │
│  └────────────────────────────┘   │
│         Tap outside to dismiss    │  Gray α=0.5, 10sp
└──────────────────────────────────┘
```

- 点击选项 → 复制到剪贴板 + Toast 提示
- 面板全宽显示 (`fillMaxWidth`)
- 垂直排列 `spacedBy(12.dp)`

### 5.3 好感度条 (Affinity Bar)

| 属性 | 值 |
|------|------|
| 高度 | 6.dp |
| 圆角 | 3.dp |
| 轨道色 | `Color.White` α=0.1 |
| 进度色 | 根据好感度区间动态变色（animateColorAsState） |
| 标签字号 | 11.sp |
| 数值范围 | 0–100，自动 clamp |

### 5.4 主操作按钮 (Start/Stop)

| 属性 | 值 |
|------|------|
| 高度 | 56.dp |
| 宽度 | fillMaxWidth |
| 文字 | 18.sp |
| 状态 | "Start Galize" / "Stop Galize" |
| 禁用条件 | 权限未授权时 disabled |
| 组件 | Material 3 `Button` |

### 5.5 权限引导按钮

| 属性 | 值 |
|------|------|
| 组件 | Material 3 `OutlinedButton` |
| 宽度 | fillMaxWidth |
| 间距 | 按钮间 16.dp |
| 逻辑 | 权限已获取时隐藏 |

### 5.6 状态卡片 (Status Card)

| 属性 | 值 |
|------|------|
| 背景 | `MaterialTheme.colorScheme.surface` |
| 内边距 | 16.dp |
| 标题 | "Status", SemiBold |
| 内容色 | 运行中=primary，停止=onSurface α=0.5 |

---

## 6. 页面结构

### 6.1 首页 (HomeScreen)

```
┌─ TopAppBar ─────────────────────┐
│  Galize            [History][⚙] │
├─────────────────────────────────┤
│                                 │
│      万物皆可 Galgame           │  24sp, Bold, primary
│   Everything is a Visual Novel  │  14sp, α=0.6
│                                 │
│   [Grant Overlay Permission]    │  仅未授权时显示
│   [Grant Notification Permission]│
│                                 │
│   ┌─ Start Galize ──────────┐   │  56dp 高
│   └──────────────────────────┘   │
│                                 │
│   ┌─ Status Card ────────────┐  │
│   │  Status                  │  │
│   │  Floating bubble active  │  │
│   └──────────────────────────┘  │
└─────────────────────────────────┘
```

### 6.2 设置页 (SettingsScreen)

```
┌─ TopAppBar ─────────────────────┐
│  ← Settings                     │
├─────────────────────────────────┤
│  AI Configuration               │  titleMedium
│  ┌─ API Base URL ────────────┐  │  OutlinedTextField
│  └───────────────────────────┘  │
│  ┌─ API Key ─────────────────┐  │  OutlinedTextField
│  └───────────────────────────┘  │
│  ─────────── Divider ──────────  │
│  Persona                        │  titleMedium
│  ○ Default (Balanced)           │  RadioButton + Text
│  ○ Cool Genius (高冷学霸)       │
│  ○ Hot Blooded (热血少年)       │
│  ○ Sharp Tongue (毒舌执事)      │
└─────────────────────────────────┘
```

### 6.3 历史页 (HistoryScreen)

```
┌─ TopAppBar ─────────────────────┐
│  ← History                      │
├─────────────────────────────────┤
│                                 │
│  (居中空状态文字)                │  onSurface α=0.5
│  No conversation history yet.   │
│  Start using Galize to see...   │
│                                 │
└─────────────────────────────────┘
```

---

## 7. 动画与过渡

### 7.1 动画规范

| 场景 | API | 时长 | 说明 |
|------|-----|------|------|
| 好感度颜色变化 | `animateColorAsState` | 默认 Spring | 区间跨越时平滑变色 |
| 面板显隐 | `AnimatedVisibility` | 200–300ms | 选项面板弹出/收起 |
| 按钮状态切换 | Material 3 内置 | — | 按压/禁用自带过渡 |
| 加载状态 | Skeleton / CircularProgressIndicator | — | 异步操作 >300ms 时显示 |

### 7.2 动画原则
- **使用 Compose 动画 API**（`animate*AsState`、`AnimatedVisibility`），禁止在 ViewModel 中处理动画逻辑
- **避免无限循环动画**用于装饰目的，仅 loading 指示器允许
- 所有过渡时长控制在 **150–300ms**
- 尊重 `prefers-reduced-motion` 系统设置

---

## 8. 交互规范

### 8.1 手势交互

| 手势 | 组件 | 行为 |
|------|------|------|
| **单击** | 悬浮球 | 触发截图 → OCR → AI → 显示选项面板 |
| **拖拽** | 悬浮球 | 在屏幕任意位置自由拖拽 |
| **单击** | 选项按钮 | 复制对应文本到剪贴板 + Toast |
| **单击外部** | 选项面板 | 关闭面板 |

### 8.2 反馈规范

| 操作 | 反馈形式 | 说明 |
|------|---------|------|
| 复制成功 | `Toast.LENGTH_SHORT` | "Copied to clipboard!" |
| 权限缺失 | `Snackbar.Long` | 错误消息提示 |
| 服务状态 | Status Card 文字 + 颜色 | 实时反映运行状态 |
| AI 生成中 | Loading indicator | >300ms 时显示进度 |
| 防重复点击 | 按钮 disabled + loading | 异步操作期间禁用 |

### 8.3 可点击元素
- 所有可交互元素必须使用 `.clickable {}` 修饰符
- 使用 `indication` 提供视觉涟漪反馈（Material 3 默认）
- 禁用状态使用 `enabled = false` 而非隐藏

---

## 9. Jetpack Compose 最佳实践

### 9.1 主题使用
- 颜色始终通过 `MaterialTheme.colorScheme.*` 引用，禁止绕过主题硬编码
- 三选项色（Green/Red/Purple）作为语义色定义在 `GalizeTheme.kt` 中
- Typography 使用 `MaterialTheme.typography.*` 标准层级

### 9.2 状态管理
- UI 状态通过 `StateFlow` + `collectAsState()` 从 ViewModel 订阅
- 一次性事件（Toast/Snackbar）通过 `LaunchedEffect` 消费
- 动画状态纯粹在 Composable 层管理

### 9.3 性能
- 长列表使用 `LazyColumn` / `LazyRow`
- 图片懒加载（Room 存储摘要时避免内存泄漏）
- Overlay 面板使用独立 `ComposeView` 挂载到 WindowManager

### 9.4 无障碍
- 所有 Icon 提供 `contentDescription`
- 表单输入绑定 `label`
- 颜色不作为唯一信息指示（配合文字标签）

---

## 10. 禁止事项（Anti-patterns）

| 禁止 | 原因 |
|------|------|
| ❌ 实现 Light Mode | 产品定位 Dark-Only，赛博朋克风格 |
| ❌ 硬编码颜色值 | 必须使用 Theme 变量 |
| ❌ Emoji 作为 UI 图标 | 使用 Material Icons / SVG |
| ❌ ViewModel 中做动画 | 动画是 UI 层关注点 |
| ❌ 无反馈的异步操作 | >300ms 必须显示 loading |
| ❌ 允许 loading 中重复点击 | 异步操作期间禁用按钮 |
| ❌ 布局偏移的 Hover/Press 效果 | 使用颜色/透明度变化，非 scale |
| ❌ 超过 500ms 的过渡动画 | 保持交互响应感 |

---

## 11. 页面覆盖规则

> 当构建特定页面时，优先检查 `design-system/pages/[page-name].md`。
> 若该文件存在，其规则 **覆盖** 本 Master 文件对应部分。
> 若不存在，严格遵循本文件规则。
