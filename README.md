# Galize

**万物皆可 Galgame — Everything is a Visual Novel**

一款基于 AI 视觉识别的"社交生存攻略"助手。通过实时屏幕监控与 AI 逻辑，将社交对话转化为带有选项和好感度反馈的 Galgame 体验。

## 核心功能

### 三位一体决策系统 (The Triple-Choice)
每次对话生成三个维度的回复建议：
- **[Pure Heart] 纯情/稳健** — 增加好感度，走王道路线
- **[Chaos] 混沌/死亡** — 极具攻击性的梗，用于整蛊或终结话题
- **[Philosopher] 哲思/跳脱** — 打破常规逻辑的意外回答

### 实时视界引擎
- 悬浮球触发截图采样
- ML Kit 中文 OCR 识别聊天内容
- 聊天气泡位置解析（区分我方/对方）

### 好感度动态仪
- 基于 AI 语义分析的实时好感度评分
- 动态渐变进度条显示

### 潜台词解码
- 分析对方消息背后的真实意图

## 技术栈

| 类别 | 选型 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 构建 | AGP 9.1.1 + Gradle 9.3.1 |
| DI | Hilt 2.59.2 |
| 网络 | Retrofit + OkHttp |
| 本地存储 | Room + DataStore |
| OCR | Google ML Kit Text Recognition v2 (Chinese) |
| AI (云端) | OpenAI-compatible API |
| AI (本地) | Heuristic fallback (planned: Google AI Edge) |
| 异步 | Kotlin Coroutines + Flow |
| JDK | 25 (LTS) |

## 项目结构

```
com.galize.app/
├── ai/              # AI 决策引擎（云端 + 本地降级）
├── di/              # Hilt 依赖注入模块
├── model/           # 数据模型 + Room 实体
├── ocr/             # OCR 识别 + 聊天解析
├── repository/      # 数据仓库层
├── service/         # 悬浮球 Service + 截图 + Pipeline
└── ui/
    ├── navigation/  # Compose Navigation
    ├── overlay/     # 悬浮面板（选项卡 + 好感度条）
    ├── screen/      # 主页 / 设置 / 历史
    ├── theme/       # 赛博朋克紫主题
    └── viewmodel/   # ViewModel 层
```

## 功能路线图

### Phase 1 — MVP 核心链路 

> 悬浮球触发截图 → OCR 文字提取 → AI 生成三选项 → 好感度反馈显示

| 模块 | 功能 | 状态 |
|------|------|------|
| 悬浮球 Service | 可拖拽悬浮球 + 前台服务生命周期管理 | done |
| 屏幕截图 | MediaProjection API 截图 + 权限引导 | done |
| OCR 引擎 | ML Kit Text Recognition v2 中文识别 | ✅ 已完成 |
| 聊天解析 | 气泡位置解析，区分我方/对方消息 | ✅ 已完成 |
| AI 决策 — 云端 | CloudAiClient（OpenAI-compatible API），JSON 格式三选项 | ✅ 已完成 |
| 悬浮结果面板 | 三色选项卡 + 好感度进度条 + 潜台词显示 | ✅ 已完成 |
| App 主界面 | 首页（权限开关）/ 设置（API Key、人设）/ 历史 | ✅ 已完成 |
| 数据持久化 | Room 对话历史 + DataStore 偏好设置 | ✅ 已完成 |
| 主题 | 赛博朋克紫 Material 3 主题 | ✅ 已完成 |

### Phase 2 — 体验增强 🚧

| 模块 | 功能 | 状态 |
|------|------|------|
| AI 本地推理 | Google AI Edge 替代启发式降级 | 📋 计划中 |
| 自动填充 | 选中选项直接填入输入框（无障碍服务） | 📋 计划中 |
| 多 App 适配 | 微信 / QQ / 抖音 等聊天布局特化解析 | 📋 计划中 |
| 好感度趋势图 | 历史页展示好感度变化曲线 | 📋 计划中 |
| 对话上下文记忆 | 多轮对话上下文窗口，提升 AI 连贯性 | 📋 计划中 |

### Phase 3 — 进阶玩法 💡

| 模块 | 功能 | 状态 |
|------|------|------|
| 动态立绘 | 根据好感度变化展示角色立绘 | 💡 构想 |
| 存档 / 结局系统 | 对话分支记录 + 多结局达成 | 💡 构想 |
| CG 墙 | 收集关键对话截图作为成就 | 💡 构想 |
| 人设市场 | 社区共享 AI 人设 Prompt 模板 | 💡 构想 |
| 语音识别 | 支持语音消息的实时转写与分析 | 💡 构想 |

## UI 设计指南

项目遵循赛博朋克风格的统一设计规范，详见 [`docs/design/ui-guide.md`](docs/design/ui-guide.md)，涵盖：

- **色彩体系** — 以 Electric Violet (`#8B5CF6`) 为主色的暗色赛博朋克调色板
- **组件规范** — 悬浮球、三选项卡片、好感度进度条、潜台词区域等核心组件的样式与交互定义
- **动效与反馈** — 点击涟漪、面板展开/收起动画、好感度渐变过渡
- **字体与排版** — 层级式字体大小、行高、间距规范

> 所有 UI 实现基于 Jetpack Compose + Material 3，主题定义位于 [`ui/theme/GalizeTheme.kt`](app/src/main/java/com/galize/app/ui/theme/GalizeTheme.kt)

## 日志规范

项目采用统一的日志体系，详见 [`docs/architecture/logging.md`](docs/architecture/logging.md)，包含：

- **统一入口** — 通过 `GalizeLogger` 封装所有日志调用，禁止直接使用 `android.util.Log`
- **崩溃捕获** — `CrashHandler` 全局异常拦截，致命错误自动记录
- **日志级别** — DEBUG / INFO / WARN / ERROR 分级策略
- **标签规范** — 按模块命名 Tag，便于 Logcat 过滤定位

> 相关实现位于 [`utils/GalizeLogger.kt`](app/src/main/java/com/galize/app/utils/GalizeLogger.kt) 和 [`utils/CrashHandler.kt`](app/src/main/java/com/galize/app/utils/CrashHandler.kt)

## 构建要求

- Android Studio Narwhal+ (2025.3+)
- JDK 25
- Android SDK 36 (minSdk 31)

## 快速开始

1. Clone 项目
2. 在 Android Studio 中打开
3. Sync Gradle
4. 在设置页配置 AI API Key 和 Base URL
5. 运行到设备，授权悬浮窗和屏幕截图权限
6. 点击悬浮球开始使用

## 权限说明

| 权限 | 用途 |
|------|------|
| `SYSTEM_ALERT_WINDOW` | 悬浮球 + 选项面板 |
| `FOREGROUND_SERVICE_MEDIA_PROJECTION` | 屏幕截图 |
| `INTERNET` | 云端 AI API 调用 |
| `POST_NOTIFICATIONS` | 前台服务通知 |

## License

MIT
