# Galize 测试框架指南

## 概述

Galize 项目采用完整的测试体系，包含单元测试、UI 测试和集成测试。测试框架于 2026-04-21 建立，使用现代化的 Kotlin 测试工具链。

## 技术栈

### 单元测试 (Unit Tests)
- **JUnit 5** (5.11.4, Jupiter) - 现代测试框架，支持嵌套测试和参数化测试
- **MockK** (1.13.16) - Kotlin 原生 Mock 框架，用于模拟依赖
- **Turbine** (1.2.0) - Kotlin Flow 测试库，简化异步流测试
- **Kotlin Coroutines Test** - 协程测试支持

### UI 测试 (Instrumented Tests)
- **Espresso** (3.6.1) - Android UI 测试框架
- **Compose UI Testing** (1.7.8) - Compose 组件测试 API
- **MockK** (1.13.16) - 也支持在 instrumented tests 中使用
- **Hilt Testing** (2.59.2) - Hilt 依赖注入测试支持

## 配置详情

### 依赖版本 (libs.versions.toml)

```toml
[versions]
junit5 = "5.11.4"
mockk = "1.13.16"
turbine = "1.2.0"
junitVersion = "1.2.1"
espressoCore = "3.6.1"
composeUiTest = "1.7.8"
hiltTesting = "2.59.2"

[libraries]
# Testing
junit5-jupiter = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "junit5" }
mockk = { group = "io.mockk", name = "mockk", version.ref = "mockk" }
turbine = { group = "app.cash.turbine", name = "turbine", version.ref = "turbine" }
androidx-junit = { group = "androidx.test.ext", name = "junit", version.ref = "junitVersion" }
androidx-espresso-core = { group = "androidx.test.espresso", name = "espresso-core", version.ref = "espressoCore" }
androidx-compose-ui-test = { group = "androidx.compose.ui", name = "ui-test-junit4", version.ref = "composeUiTest" }
androidx-compose-ui-test-manifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
hilt-android-testing = { group = "com.google.dagger", name = "hilt-android-testing", version.ref = "hiltTesting" }
```

### 构建配置 (app/build.gradle.kts)

```kotlin
android {
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    // Unit Testing - JUnit5
    testImplementation(libs.junit5.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    
    // Mocking - MockK
    testImplementation(libs.mockk)
    androidTestImplementation(libs.mockk)
    
    // Flow Testing - Turbine
    testImplementation(libs.turbine)
    
    // Android Instrumented Testing
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    
    // Hilt Testing
    testImplementation(libs.hilt.android.testing)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.android.compiler)
    kspTest(libs.hilt.android.compiler)
}
```

## 测试目录结构

```
app/src/
├── test/java/com/galize/app/          # 单元测试
│   ├── repository/
│   │   └── ChatRepositoryTest.kt      # Repository 层测试
│   ├── ui/viewmodel/
│   │   └── HomeViewModelTest.kt       # ViewModel 测试
│   └── ocr/
│       └── ChatMessageParserTest.kt   # 工具类测试
│
└── androidTest/java/com/galize/app/   # UI/集成测试
    └── ui/screen/
        └── HomeScreenUiTest.kt        # Compose UI 测试
```

## 已实现的测试

### 单元测试 (test/)

#### 1. ChatRepositoryTest (239 行)
**位置**: `app/src/test/java/com/galize/app/repository/ChatRepositoryTest.kt`

**测试覆盖**:
- ✅ getAllConversations - Flow 数据流测试
- ✅ createConversation - 参数捕获和验证
- ✅ updateConversation - suspend 方法测试
- ✅ getChatLogs - Flow 过滤和排序
- ✅ saveChatLog / saveChatLogs - 批量操作测试

**技术要点**:
- 使用 MockK 模拟 AppDatabase 和 DAO
- 使用 Turbine 测试 Flow 发射
- 使用 `coEvery` 配置 suspend 方法
- 使用 `slot<>` 捕获方法参数

#### 2. HomeViewModelTest (124 行)
**位置**: `app/src/test/java/com/galize/app/ui/viewmodel/HomeViewModelTest.kt`

**测试覆盖**:
- ✅ 初始状态验证
- ✅ StateFlow 状态变化
- ✅ 错误消息清除

**技术要点**:
- 使用 `runTest` 进行协程测试
- 使用 `StandardTestDispatcher` 控制协程执行
- 测试 StateFlow 的初始值和状态转换

#### 3. ChatMessageParserTest (345 行)
**位置**: `app/src/test/java/com/galize/app/ocr/ChatMessageParserTest.kt`

**测试覆盖**:
- ✅ 空输入处理
- ✅ 消息所有权检测（左右边判断）
- ✅ 消息垂直排序
- ✅ 位置坐标追踪
- ✅ 不同屏幕宽度适配
- ✅ 边界场景（阈值、空白、大量消息）

**技术要点**:
- 纯单元测试，无需 Mock
- 嵌套测试组织（@Nested）
- 全面的边界条件覆盖

### UI 测试 (androidTest/)

#### HomeScreenUiTest (127 行)
**位置**: `app/src/androidTest/java/com/galize/app/ui/screen/HomeScreenUiTest.kt`

**测试覆盖**:
- ✅ 标题和副标题显示
- ✅ 状态卡片显示
- ✅ 启动按钮显示
- ✅ 导航按钮存在性
- ✅ 设置导航回调
- ✅ 历史记录导航回调

**技术要点**:
- 使用 `createComposeRule()` 设置测试环境
- 使用语义选择器查找节点
- 验证 UI 交互和回调

## 运行测试

### 运行所有单元测试
```bash
./gradlew test
```

### 运行所有 instrumented 测试
```bash
./gradlew connectedAndroidTest
```

### 运行特定测试类
```bash
# 单元测试
./gradlew test --tests "com.galize.app.repository.ChatRepositoryTest"

# Instrumented 测试
./gradlew connectedAndroidTest --tests "com.galize.app.ui.screen.HomeScreenUiTest"
```

### 运行特定测试方法
```bash
./gradlew test --tests "com.galize.app.repository.ChatRepositoryTest.createConversation"
```

## 编写测试示例

### 1. Repository 测试 (MockK + Turbine)

```kotlin
@Test
fun `should return flow of conversations from dao`() = runBlocking {
    // Given
    val expectedConversations = listOf(
        ConversationEntity(id = 1, appType = "WECHAT")
    )
    every { conversationDao.getAllConversations() } returns flowOf(expectedConversations)

    // When & Then
    repository.getAllConversations().test {
        val result = awaitItem()
        assertEquals(expectedConversations, result)
        awaitComplete()
    }
}
```

**关键点：**
- 使用 `mockk()` 创建 mock 对象
- 使用 `every { } returns` 配置同步方法
- 使用 `coEvery { } returns` 配置 suspend 方法
- 使用 Turbine 的 `.test { }` 测试 Flow
- 使用 `awaitItem()` 获取发射值
- 使用 `awaitComplete()` 验证 Flow 完成

### 2. ViewModel 测试 (StateFlow + Turbine)

```kotlin
@Test
fun `should have correct initial state`() = runTest {
    viewModel.isServiceRunning.test {
        assertFalse(awaitItem())
        cancelAndIgnoreRemainingEvents()
    }
}
```

**关键点：**
- 使用 `runTest` 替代 `runBlocking` 进行协程测试
- 使用 `StandardTestDispatcher` 控制协程执行
- 测试 StateFlow 的初始状态和状态变化

### 3. Compose UI 测试

```kotlin
@Test
fun homeScreen_shouldDisplayTitleAndSubtitle() {
    // Given
    composeTestRule.setContent {
        GalizeTheme {
            HomeScreen(
                onNavigateToSettings = {},
                onNavigateToHistory = {}
            )
        }
    }

    // Then
    composeTestRule.onNodeWithText("万物皆可 Galgame").assertIsDisplayed()
    composeTestRule.onNodeWithText("Everything is a Visual Novel").assertIsDisplayed()
}
```

**关键点：**
- 使用 `createComposeRule()` 创建测试规则
- 使用 `setContent` 设置 Compose 内容
- 使用 `onNodeWithText` 查找文本节点
- 使用 `onNodeWithContentDescription` 查找图标按钮
- 使用 `performClick()` 模拟点击
- 使用 `assertIsDisplayed()` 验证可见性

## 测试统计

| 测试文件 | 类型 | 行数 | 测试用例数 | 覆盖率目标 |
|---------|------|------|-----------|----------|
| ChatRepositoryTest | 单元测试 | 239 | 9 | 80%+ |
| HomeViewModelTest | 单元测试 | 124 | 3 | 70%+ |
| ChatMessageParserTest | 单元测试 | 345 | 14 | 90%+ |
| HomeScreenUiTest | UI 测试 | 127 | 6 | 关键路径 |
| **总计** | - | **835** | **32** | - |

## 测试最佳实践

### 1. 使用嵌套测试组织代码
```kotlin
@Nested
@DisplayName("createConversation Tests")
inner class CreateConversationTests {
    @Test
    fun `should create conversation and return id`() { ... }
}
```

### 2. 使用描述性测试名称
- ✅ `should return empty list when no conversations exist`
- ❌ `test1`, `testEmptyList`

### 3. 遵循 AAA 模式
- **Arrange** (Given) - 准备测试数据
- **Act** (When) - 执行被测操作
- **Assert** (Then) - 验证结果

### 4. MockK 常用语法
```kotlin
// 创建 Mock
val mockDao = mockk<ConversationDao>()

// 配置同步方法
every { dao.getAll() } returns flowOf(list)

// 配置 suspend 方法
coEvery { dao.insert(any()) } returns 1L

// 配置 void 方法
coEvery { dao.update(any()) } just Runs

// 捕获参数
val captured = slot<ConversationEntity>()
coEvery { dao.insert(capture(captured)) } returns 1L

// 验证调用
verify(exactly = 1) { dao.getAll() }
coVerify { dao.insert(any()) }
verifyOrder { ... }

// 清理
clearAllMocks()
```

### 5. Turbine Flow 测试
```kotlin
flow.test {
    // 获取下一个发射值
    val item = awaitItem()
    
    // 验证 Flow 完成
    awaitComplete()
    
    // 验证 Flow 抛出异常
    val error = awaitError()
    
    // 取消并忽略剩余事件
    cancelAndIgnoreRemainingEvents()
}
```

### 6. Compose 测试选择器
```kotlin
// 文本
onNodeWithText("Hello")
onNodeWithContentDescription("Settings icon")

// 语义属性
onNode(hasText("Hello") and hasClickAction())

// 断言
assertIsDisplayed()
assertIsEnabled()
assertTextEquals("Hello")

// 操作
performClick()
performTextInput("New text")
performScrollTo()
```

## 测试覆盖率

### 查看测试覆盖率
```bash
# 需要先在 build.gradle.kts 中启用 JaCoCo
./gradlew jacocoTestReport
```

### 目标覆盖率
- **Repository 层**: 80%+
- **ViewModel 层**: 70%+
- **工具类/解析器**: 90%+
- **UI 层**: 关键路径覆盖

## 已知问题和注意事项

### 1. 主代码编译问题
当前主代码存在 `GalizeLogger` 引用问题，需要先修复才能运行测试。这是代码库现有问题，与测试框架配置无关。

### 2. Compose UI 测试版本
- `ui-test-junit4` 必须使用具体版本号（如 1.7.8），不能使用 Compose BOM 版本号
- 参考：[常见问题 - Compose UI测试依赖版本需匹配BOM版本](../../../docs/dev/common-issues.md)

### 3. 协程测试
- 使用 `runTest` 而非 `runBlocking` 进行协程测试
- 必须配置 `TestDispatcher` 以控制协程执行时机

### 4. Instrumented 测试运行要求
- 需要连接 Android 设备或启动模拟器
- API Level 31+ (Android 12+)
- 建议至少 2GB 可用内存

## 常见问题

### Q: 测试运行时出现 "No tests found"
A: 确保：
1. 测试类使用 `@Test` 注解
2. 使用 JUnit 5 的 `org.junit.jupiter.api.Test`
3. build.gradle.kts 中正确配置了 JUnit 5

### Q: Flow 测试超时
A: 使用 `runTest` 并配置测试调度器：
```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
@Test
fun testFlow() = runTest {
    val testDispatcher = StandardTestDispatcher()
    Dispatchers.setMain(testDispatcher)
    // ... 测试代码
}
```

### Q: MockK 验证失败
A: 检查：
1. 参数匹配器是否正确 (`any()`, `match { }`, `eq()`)
2. 是否使用了正确的验证方法 (`verify` vs `coVerify`)
3. Mock 配置是否在执行前完成

### Q: Compose 测试找不到节点
A: 确保：
1. 节点在测试执行时已渲染
2. 使用正确的选择器
3. 如果需要，先执行滚动操作 `performScrollTo()`

## CI/CD 集成

测试已集成到 GitHub Actions 工作流中：
- 单元测试在每次 PR 时自动运行
- Instrumented 测试需要 Android 模拟器环境

## 后续改进

- [ ] 添加 Robolectric 支持（无需设备的 Android 测试）
- [ ] 配置 JaCoCo 代码覆盖率报告
- [ ] 添加快照测试（Compose UI 截图对比）
- [ ] 增加集成测试（完整用户流程）
- [ ] 添加性能测试

## 参考资料

### 官方文档
- [JUnit 5 用户指南](https://junit.org/junit5/docs/current/user-guide/)
- [MockK 文档](https://mockk.io/)
- [Turbine GitHub](https://github.com/cashapp/turbine)
- [Compose 测试文档](https://developer.android.com/jetpack/compose/testing)
- [Android 测试指南](https://developer.android.com/training/testing)

### 内部文档
- [架构设计文档](../architecture/)
- [UI 设计规范](../design/ui-guide.md)
- [日志规范](../architecture/logging.md)

## 更新历史

| 日期 | 版本 | 更新内容 | 作者 |
|------|------|---------|------|
| 2026-04-21 | 1.0.0 | 初始版本，建立完整测试框架 | AI Assistant |

### 版本 1.0.0 详情
- ✅ 配置 JUnit 5 + MockK + Turbine + Espresso 技术栈
- ✅ 创建 4 个测试文件，共 835 行测试代码
- ✅ 实现 32 个测试用例
- ✅ 编写完整的测试指南文档
- ✅ 提供测试示例和最佳实践
