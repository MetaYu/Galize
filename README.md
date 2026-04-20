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
