# Galize 架构设计文档

## 系统架构概览

```
┌─────────────────────────────────────────────────────────┐
│                    用户界面层 (UI)                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌─────────┐ │
│  │ HomeScreen│  │Settings  │  │ History  │  │ Overlay │ │
│  │          │  │ Screen   │  │ Screen   │  │ Panels  │ │
│  └──────────┘  └──────────┘  └──────────┘  └─────────┘ │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│                  ViewModel 层                           │
│  ┌──────────────────┐  ┌──────────────────────┐        │
│  │  HomeViewModel   │  │  SettingsViewModel   │        │
│  └──────────────────┘  └──────────────────────┘        │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│                  业务逻辑层                              │
│  ┌──────────────────────────────────────────────────┐   │
│  │          FloatingBubbleService                   │   │
│  │  ┌────────┐  ┌─────┐  ┌──────┐  ┌────────────┐ │   │
│  │  │Screen  │→│ OCR │→│Parser│→│  AI Client  │ │   │
│  │  │Capture │  │     │  │      │  │ (Cloud/    │ │   │
│  │  │        │  │     │  │      │  │  Local)    │ │   │
│  │  └────────┘  └─────┘  └──────┘  └────────────┘ │   │
│  └──────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│                  数据访问层                              │
│  ┌──────────────────┐  ┌──────────────────────┐        │
│  │  ChatRepository  │  │  SettingsRepository  │        │
│  │  (Room)          │  │  (DataStore)         │        │
│  └──────────────────┘  └──────────────────────┘        │
└─────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────────────────────────────────────┐
│                  数据存储层                              │
│  ┌──────────────────┐  ┌──────────────────────┐        │
│  │  SQLite (Room)   │  │  DataStore (XML)     │        │
│  └──────────────────┘  └──────────────────────┘        │
└─────────────────────────────────────────────────────────┘
```

## 模块设计

### 1. 服务层 (Service Layer)

#### FloatingBubbleService
**职责**: 前台服务，管理悬浮球和结果面板

**核心流程**:
```kotlin
onBubbleTapped()
  ↓
ScreenCaptureManager.captureScreen()
  ↓
OcrEngine.recognizeText()
  ↓
ChatMessageParser.parse()
  ↓
AiClient.generateChoices()
  ↓
showChoicePanel()
```

**生命周期管理**:
- `onCreate()`: 初始化服务，创建悬浮球
- `onStartCommand()`: 接收屏幕录制 Intent
- `onDestroy()`: 清理资源，停止服务

**并发控制**:
- 使用 `CoroutineScope(Dispatchers.IO + SupervisorJob())`
- 确保后台任务不会因单个失败而中断
- Service 销毁时自动取消所有协程

#### ScreenCaptureManager
**职责**: 屏幕截图实现

**关键设计**:
- 每次截图创建新的 MediaProjection 实例
- VirtualDisplay 将屏幕内容输出到 ImageReader
- 150ms 延迟等待帧渲染完成
- 自动裁剪到实际屏幕尺寸

**资源管理**:
```kotlin
captureScreen() {
  try {
    // 创建 MediaProjection
    // 创建 VirtualDisplay
    // 等待帧渲染
    // 获取并处理 bitmap
  } finally {
    releaseCapture()  // 确保资源释放
  }
}
```

#### ScreenCapturePermissionManager
**职责**: 权限请求管理

**权限流程**:
1. 创建权限请求 Intent
2. Activity 启动 Intent
3. 处理用户授权结果
4. 传递 resultCode 和 data 给 Service

### 2. AI 层 (AI Layer)

#### AiClient 接口
```kotlin
interface AiClient {
    suspend fun isAvailable(): Boolean
    suspend fun generateChoices(context: ConversationContext): Result<ChoiceResult>
}
```

#### CloudAiClient
**实现**: OpenAI-compatible API 客户端

**配置**:
- API Key (必需)
- Base URL (默认: https://api.openai.com/v1)
- Model (默认: gpt-4o-mini)

**请求流程**:
```
1. PromptBuilder.buildPrompt(context)
2. 构建 HTTP 请求 (OkHttp)
3. 发送请求到 API
4. 解析 JSON 响应 (Gson)
5. 返回 ChoiceResult
```

**错误处理**:
- API Key 未配置 → 降级到 LocalAiClient
- 网络错误 → 重试或降级
- JSON 解析失败 → 返回默认选项

#### LocalAiClient
**实现**: 离线启发式降级

**策略**:
- 基于关键词的模板匹配
- 随机选择增加多样性
- 固定回复，无上下文理解

**Phase 2 改进**:
- 集成 Google AI Edge
- 支持本地小模型推理

#### PromptBuilder
**职责**: 构建 AI 提示词

**系统提示词**:
- 定义三种回复类型
- 指定 JSON 输出格式
- 设定好感度规则

**用户提示词**:
- 包含对话历史
- 标注消息来源 ([Me]/[Them])
- 指定生成任务

### 3. OCR 层 (OCR Layer)

#### OcrEngine
**技术**: Google ML Kit Text Recognition

**处理流程**:
```
Bitmap → InputImage → TextRecognizer → TextBlocks
```

**性能优化**:
- 异步处理 (suspend function)
- 支持取消 (CancellableCoroutine)
- 错误返回空列表而非抛出异常

**数据结构**:
```kotlin
OcrTextBlock {
  text: String           // 识别文本
  boundingBox: Rect?     // 位置信息
  confidence: Float?     // 置信度
  lines: List<OcrTextLine>
}
```

#### ChatMessageParser
**算法**: 基于位置的启发式解析

**判断逻辑**:
```kotlin
centerX = block.boundingBox.centerX()
isFromMe = centerX > screenWidth * 0.55
```

**过滤规则**:
- 移除空文本
- 移除过短文本 (< 2 字符)
- 按垂直位置排序

**局限性**:
- 仅支持双方对话
- 假设标准聊天布局
- 无法识别居中消息

### 4. 数据层 (Data Layer)

#### Room 数据库

**Entity 设计**:
```kotlin
@Entity(tableName = "conversations")
data class ConversationEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val startedAt: Long,
  val endedAt: Long?,
  val appType: String,
  val totalAffinity: Int,
  val messageCount: Int,
  val summary: String
)

@Entity(tableName = "chat_logs")
data class ChatLogEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val conversationId: Long,
  val text: String,
  val isFromMe: Boolean,
  val timestamp: Long,
  val chosenReply: String?,
  val choiceType: String?
)
```

**DAO 设计**:
- 使用 Flow 实现响应式查询
- suspend functions 用于写操作
- 支持批量插入

#### DataStore 偏好

**存储键**:
```kotlin
KEY_API_KEY: String          // API Key
KEY_API_BASE_URL: String     // API 端点
KEY_AI_MODEL: String         // AI 模型
KEY_PERSONA: String          // 用户人设
KEY_AFFINITY: Int            // 好感度
```

**优势** (vs SharedPreferences):
- 类型安全
- 支持 Flow
- 自动迁移
- 事务性更新

### 5. UI 层 (UI Layer)

#### Jetpack Compose 架构

**主题**:
- Material 3 设计系统
- 赛博朋克紫配色方案
- 深色模式优先

**主要组件**:

1. **FloatingBubbleContent**
   - 可拖拽悬浮球
   - 点击触发分析
   - 动画反馈

2. **ChoicePanel**
   - 三选项卡片
   - 好感度进度条
   - 潜台词显示
   - 点击复制

3. **HomeScreen**
   - 权限检查引导
   - 服务控制按钮
   - 状态显示
   - 导航入口

4. **SettingsScreen**
   - API Key 配置
   - 模型选择
   - 人设设置
   - 数据管理

5. **HistoryScreen**
   - 对话历史列表
   - 好感度趋势
   - 详情查看

#### ViewModel 模式

**HomeViewModel**:
- 管理服务状态
- 权限检查逻辑
- 错误处理

**SettingsViewModel**:
- 加载/保存设置
- 验证输入
- 提供状态流

## 依赖注入 (Hilt)

### Module 配置

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "galize_database"
        ).build()
    }
    
    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepository(context)
    }
    
    @Provides
    @Singleton
    fun provideChatRepository(database: AppDatabase): ChatRepository {
        return ChatRepository(database)
    }
    
    @Provides
    @Singleton
    fun providePromptBuilder(): PromptBuilder {
        return PromptBuilder()
    }
}
```

### 注入点

- **Activity**: `@AndroidEntryPoint`
- **Service**: `@AndroidEntryPoint`
- **ViewModel**: `@HiltViewModel` + `hiltViewModel()`
- **Repository**: `@Inject constructor()`
- **AI Clients**: `@Singleton` + `@Inject constructor()`

## 协程使用

### Scope 管理

1. **Service Scope**:
   ```kotlin
   private val serviceScope = CoroutineScope(
       Dispatchers.IO + SupervisorJob()
   )
   ```
   - 用于后台长时间任务
   - Service 销毁时取消

2. **ViewModel Scope**:
   ```kotlin
   viewModelScope.launch { ... }
   ```
   - 用于 UI 相关异步操作
   - ViewModel 清除时自动取消

3. **Suspend Functions**:
   ```kotlin
   suspend fun recognizeText(bitmap: Bitmap): List<OcrTextBlock>
   ```
   - 可取消的异步操作
   - 支持协程异常处理

### 调度器选择

- **Dispatchers.IO**: I/O 操作 (网络、数据库、文件)
- **Dispatchers.Main**: UI 更新
- **Dispatchers.Default**: CPU 密集型计算

## 错误处理策略

### 分层错误处理

1. **Service 层**:
   - 捕获所有异常
   - Toast 通知用户
   - 记录日志

2. **AI 层**:
   - Result<T> 封装
   - 云端失败 → 本地降级
   - 重试机制

3. **OCR 层**:
   - 失败返回空列表
   - 不抛出异常
   - 记录错误日志

4. **UI 层**:
   - Snackbar 显示错误
   - ViewModel 状态管理
   - 用户友好提示

### 日志系统

**GalizeLogger**:
```kotlin
class GalizeLogger(private val tag: String) {
    fun D(message: String)  // DEBUG
    fun I(message: String)  // INFO
    fun W(message: String)  // WARN
    fun E(message: String, e: Exception?)  // ERROR
}
```

**日志级别**:
- DEBUG: 开发调试信息
- INFO: 重要流程节点
- WARN: 潜在问题
- ERROR: 错误和异常

**标签规范**:
- 按模块命名 (e.g., "ScreenCapture", "CloudAiClient")
- 便于 Logcat 过滤

## 性能优化

### 1. 截图优化
- 使用 ImageReader 而非 Bitmap 拷贝
- 及时释放 VirtualDisplay
- 避免并发截图

### 2. OCR 优化
- ML Kit 自动优化
- 支持取消操作
- 异步处理

### 3. AI 优化
- 设置 maxTokens 限制响应长度
- 超时配置防止长时间等待
- 本地降级保证可用性

### 4. UI 优化
- Compose 重组优化
- 懒加载列表
- 避免不必要的状态提升

## 安全考虑

### 1. API Key 保护
- 存储在 DataStore (内部存储)
- 不上传到服务器
- 用户自行管理

### 2. 屏幕录制
- 仅在用户触发时截图
- 截图立即处理，不保存
- 处理完成后回收 Bitmap

### 3. 数据隐私
- 所有数据本地存储
- AI 请求仅发送匿名上下文
- HTTPS 加密传输

## 测试策略

### 1. 单元测试
- Repository 层测试
- AI Client Mock 测试
- Parser 逻辑测试

### 2. 集成测试
- 数据库操作测试
- DataStore 读写测试
- Service 生命周期测试

### 3. UI 测试
- Compose 组件测试
- 用户交互流程测试
- 权限引导测试

## 未来架构演进

### Phase 2
- 添加无障碍服务层
- 多 App 适配策略模式
- 缓存层优化

### Phase 3
- 模块化拆分 (feature modules)
- 动态特性交付
- 云同步 (可选)

---

**文档版本**: 1.0  
**最后更新**: 2026-04-21
