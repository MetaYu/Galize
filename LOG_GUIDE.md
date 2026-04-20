# Galize 日志系统使用指南

## 日志系统概述

Galize 现已集成完整的日志系统和错误提示机制，包括：

1. **GalizeLogger** - 统一日志工具类
2. **CrashHandler** - 全局异常捕获器（保存崩溃日志）
3. **关键组件日志** - Application、Activity、ViewModel、Service
4. **用户友好提示** - Snackbar 显示错误消息，不再直接闪退

## 如何查看日志

### 方法 1: 使用 Android Studio Logcat（推荐）

1. 运行应用后，打开 Android Studio 底部的 **Logcat** 窗口
2. 在过滤器中输入：`Galize`
3. 你将看到所有 Galize 应用的日志

**日志级别说明：**
- `V` (Verbose) - 详细日志
- `D` (Debug) - 调试日志
- `I` (Info) - 信息日志
- `W` (Warning) - 警告日志
- `E` (Error) - 错误日志

### 方法 2: 使用 ADB 命令行

```powershell
# 查看所有 Galize 日志
adb logcat -s Galize

# 查看带时间的完整日志
adb logcat -v time -s Galize

# 只查看错误日志
adb logcat -s Galize:E

# 实时跟踪日志
adb logcat -s Galize | Select-String "Galize"
```

### 方法 3: 查看崩溃日志文件

当应用发生崩溃时，日志会自动保存到设备存储：

**文件位置：**
```
Android/data/com.galize.app/files/crash_logs/crash_YYYY-MM-DD_HH-mm-ss.log
```

**查看方式：**
1. 使用 Android Studio 的 Device File Explorer
2. 或使用 ADB 命令：
```powershell
# 拉取崩溃日志到电脑
adb pull /sdcard/Android/data/com.galize.app/files/crash_logs/ ./crash_logs/
```

## 错误提示机制

### 不再直接闪退！

当发生错误时，应用会通过以下方式提示用户：

1. **Snackbar 提示** - 在屏幕底部显示错误消息
2. **日志记录** - 详细错误信息记录到 Logcat 和文件
3. **崩溃日志** - 严重错误保存到文件供调试

### 常见错误提示

| 错误 | 提示消息 | 解决方法 |
|------|---------|----------|
| 缺少悬浮窗权限 | "请先授予悬浮窗权限" | 点击按钮跳转设置 |
| 缺少通知权限 | "请先授予通知权限（Android 13+必需）" | 点击按钮授权 |
| 缺少媒体投影权限 | "请先授予媒体投影权限（Android 14+必需）" | 点击按钮授权 |
| 服务启动失败 | "启动失败：[错误原因]" | 查看日志详情 |

### 应用启动
- `GalizeApplication.onCreate()` - 应用初始化
- `MainActivity.onCreate/onStart/onResume` - Activity 生命周期

### 权限检查
- `HomeViewModel.checkPermissions()` - 权限检查结果

### 服务控制
- `HomeViewModel.toggleService()` - 启动/停止服务
- `FloatingBubbleService.onCreate` - 服务创建
- `FloatingBubbleService.onStartCommand` - 服务启动命令
- `FloatingBubbleService.onDestroy` - 服务销毁

### 悬浮球操作
- `showBubble()` - 显示悬浮球
- `onBubbleTapped()` - 点击悬浮球
- `removeBubble()` - 移除悬浮球

## 排查崩溃的步骤

1. **复现崩溃** - 重现导致崩溃的操作
2. **查看 Logcat** - 过滤 `Galize` 标签，找到最后的错误日志
3. **检查崩溃日志文件** - 如果有未捕获异常，会生成 .log 文件
4. **分析堆栈信息** - 日志包含完整的异常堆栈跟踪

## 示例日志输出

```
2026-04-21 10:30:15.123  I  Galize: GalizeApplication initialized
2026-04-21 10:30:15.456  I  Galize: MainActivity onCreate
2026-04-21 10:30:15.789  I  Galize: MainActivity onStart
2026-04-21 10:30:15.790  I  Galize: MainActivity onResume
2026-04-21 10:30:16.123  D  Galize: Permission check result: hasOverlayPermission=true
2026-04-21 10:30:20.456  I  Galize: toggleService called, current running state: false
2026-04-21 10:30:20.457  I  Galize: Starting FloatingBubbleService
2026-04-21 10:30:20.789  I  Galize: FloatingBubbleService onCreate
2026-04-21 10:30:20.790  D  Galize: Creating notification channel
2026-04-21 10:30:20.800  D  Galize: Showing floating bubble
2026-04-21 10:30:20.850  D  Galize: Floating bubble added to window
2026-04-21 10:30:20.851  I  Galize: FloatingBubbleService started successfully
```

## 添加自定义日志

在任何代码位置添加日志：

```kotlin
import com.galize.app.utils.GalizeLogger

// 调试日志
GalizeLogger.d("调试信息")

// 信息日志
GalizeLogger.i("普通信息")

// 警告日志
GalizeLogger.w("警告信息")

// 错误日志（带异常）
GalizeLogger.e("错误信息", exception)

// 格式化异常
GalizeLogger.e(GalizeLogger.formatException(exception))
```

## 常见问题

**Q: Logcat 中没有看到日志？**
A: 确保过滤器设置为 `Galize`，并且应用正在运行。

**Q: 崩溃日志文件在哪里？**
A: 在设备的外部存储目录：`Android/data/com.galize.app/files/crash_logs/`

**Q: 如何导出日志？**
A: 在 Android Studio Logcat 中点击导出按钮，或使用 `adb logcat > log.txt`
