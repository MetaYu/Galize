# Galize MVP 技术实现路径

## MVP 范围定义

核心链路：**悬浮球触发截图 -> OCR 文字提取 -> AI 生成三选项 -> 好感度反馈显示**

MVP 不包含：动态立绘、存档/结局系统、CG 墙、自动填充（降级为复制到剪贴板）

---

## Task 1: 项目基础设施搭建

**目标**：配置 Kotlin + Jetpack Compose + 必要依赖

- 添加 Kotlin 插件到 `build.gradle.kts`
- 配置 Compose 编译器和依赖
- 添加核心依赖：Hilt (DI)、Room (本地存储)、Retrofit/OkHttp (网络)、ML Kit (OCR)
- 建立基础包结构：

```
com.example.galize/
├── ui/           # Compose UI 层
├── service/      # 悬浮窗 & 截图 Service
├── ocr/          # OCR 识别模块
├── ai/           # AI 调用层（云端+本地）
├── model/        # 数据模型
├── repository/   # 数据仓库
└── di/           # 依赖注入
```

**关键文件**：
- `gradle/libs.versions.toml` - 添加所有版本声明
- `app/build.gradle.kts` - 启用 Compose、Kotlin、Hilt

---

## Task 2: 悬浮球 Service + 截图能力

**目标**：实现一个常驻悬浮球，点击后触发屏幕截图

- 实现 `FloatingBubbleService` (前台 Service + WindowManager)
- 使用 `MediaProjection API` 获取屏幕截图权限
- 截图后裁剪聊天区域（先做全屏，后续迭代智能裁剪）
- 悬浮球 UI：Compose 渲染的圆形按钮，可拖拽

**权限需求**：
- `SYSTEM_ALERT_WINDOW` (悬浮窗)
- `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PROJECTION`
- MediaProjection 用户授权

**关键文件**：
- `AndroidManifest.xml` - 权限声明 + Service 注册
- `service/FloatingBubbleService.kt`
- `service/ScreenCaptureManager.kt`

---

## Task 3: OCR 文字识别模块

**目标**：从截图中提取聊天文字内容

- 集成 Google ML Kit Text Recognition v2（支持中文）
- 实现 `OcrEngine` 接口，输入 Bitmap 输出结构化文本
- 基础隐私遮罩：识别前模糊化图片顶部（头像区域）
- 聊天气泡解析：区分"我方"和"对方"消息（基于位置左右判断）

**依赖**：`com.google.mlkit:text-recognition-chinese`

**关键文件**：
- `ocr/OcrEngine.kt` - OCR 接口与实现
- `ocr/ChatMessageParser.kt` - 将 OCR 文本结构化为对话列表

---

## Task 4: AI 决策引擎（混合模式）

**目标**：基于对话上下文生成三选项回复

### 云端模式（默认）
- 接入 OpenAI-compatible API（支持切换 Gemini/Claude/DeepSeek）
- Prompt 工程：系统提示词定义三种风格（Pure Heart / Chaos / Philosopher）
- 流式输出支持（用户体验更好）

### 本地降级模式（离线）
- 集成 Google AI Edge (MediaPipe LLM Inference) 或 ONNX Runtime
- 使用 Gemma 2B / Phi-3-mini 等轻量模型
- 降级时仅提供基础回复建议（质量有限但可用）

### 好感度评估
- AI 返回结果附带好感度评分 (-10 ~ +10)
- 本地累计计算总好感度值

**关键文件**：
- `ai/AiClient.kt` - 统一 AI 接口
- `ai/CloudAiClient.kt` - 云端 API 实现
- `ai/LocalAiClient.kt` - 本地模型实现
- `ai/PromptBuilder.kt` - Prompt 模板管理
- `model/ChoiceResult.kt` - 三选项数据模型

---

## Task 5: 主 UI - 悬浮结果面板

**目标**：以悬浮窗形式展示 AI 生成的三选项 + 好感度

- Compose 悬浮面板：半透明卡片，展示三个选项按钮
- 选项配色：绿色(Pure Heart) / 红色(Chaos) / 紫色(Philosopher)
- 好感度条：顶部渐变进度条
- 点击选项 -> 复制到剪贴板 + Toast 提示
- 潜台词显示：选项下方灰色小字（对方意图分析）

**关键文件**：
- `ui/overlay/ChoicePanel.kt` - 三选项面板
- `ui/overlay/AffinityBar.kt` - 好感度条
- `ui/theme/GalizeTheme.kt` - 主题配色（赛博朋克紫为主）

---

## Task 6: 主界面 (App 内)

**目标**：App 启动页面，用于权限管理、设置、历史记录

- 首页：开关悬浮球 + 权限引导 + 快速设置
- 设置页：API Key 配置、人设选择、主题切换
- 历史页：过往对话摘要列表（Room 本地存储）

**关键文件**：
- `ui/screen/HomeScreen.kt`
- `ui/screen/SettingsScreen.kt`
- `ui/screen/HistoryScreen.kt`
- `ui/navigation/NavGraph.kt`

---

## Task 7: 数据持久化

**目标**：保存对话历史和用户设置

- Room 数据库：存储对话记录、好感度历史、用户设置
- DataStore：轻量偏好设置（API Key、人设选择等）
- 暂不做向量数据库（MVP 后期迭代加入 RAG）

**关键文件**：
- `repository/ChatRepository.kt`
- `model/db/AppDatabase.kt`
- `model/db/ConversationEntity.kt`

---

## 实施顺序与里程碑

| 阶段 | 内容 | 预估工作量 |
|------|------|-----------|
| Task 1 | 基础设施搭建 | 1 天 |
| Task 2 | 悬浮球 + 截图 | 2-3 天 |
| Task 3 | OCR 识别 | 1-2 天 |
| Task 4 | AI 引擎 | 2-3 天 |
| Task 5 | 悬浮结果面板 | 2 天 |
| Task 6 | App 主界面 | 2 天 |
| Task 7 | 数据持久化 | 1 天 |

**总计**：约 11-14 天可完成 MVP

---

## 技术选型总结

| 类别 | 选型 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose |
| DI | Hilt |
| 网络 | Retrofit + OkHttp |
| 本地存储 | Room + DataStore |
| OCR | ML Kit Text Recognition v2 |
| 云端 AI | OpenAI-compatible API (可切换) |
| 本地 AI | Google AI Edge / MediaPipe LLM |
| 异步 | Kotlin Coroutines + Flow |
| 导航 | Compose Navigation |
