package com.galize.app.utils

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * 全局异常捕获器
 * 捕获所有未处理的异常，记录到日志文件和控制台
 * 显示友好的错误提示对话框
 */
class CrashHandler private constructor() : Thread.UncaughtExceptionHandler {
    private val TAG = "CrashHandler"
    private var defaultHandler: Thread.UncaughtExceptionHandler? = null
    private var context: Context? = null
    private var logDir: File? = null
    
    // 给对话框显示留出时间
    private val DIALOG_DELAY = 2000L

    companion object {
        @Volatile
        private var INSTANCE: CrashHandler? = null

        fun getInstance(): CrashHandler {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CrashHandler().also { INSTANCE = it }
            }
        }
    }

    /**
     * 初始化异常捕获器
     */
    fun init(context: Context) {
        this.context = context.applicationContext
        defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(this)
        setupLogDirectory()
    }

    /**
     * 设置日志目录
     */
    private fun setupLogDirectory() {
        val baseDir = context?.getExternalFilesDir(null) ?: return
        logDir = File(baseDir, "crash_logs")
        if (!logDir!!.exists()) {
            logDir!!.mkdirs()
        }
    }

    /**
     * 当发生未捕获异常时调用
     */
    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
        val message = """
            ========================================
            CRASH REPORT
            ========================================
            Time: $timestamp
            Device: ${Build.MODEL}
            Android Version: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})
            App Version: ${getAppVersion()}
            Thread: ${thread.name}
            ========================================
        """.trimIndent()

        Log.e(TAG, message)
        Log.e(TAG, GalizeLogger.formatException(throwable))

        // 保存到文件
        saveCrashLog(timestamp, message, throwable)
        
        // 显示友好的错误提示
        showErrorDialog(throwable)
        
        // 等待一段时间后让系统处理
        try {
            Thread.sleep(DIALOG_DELAY)
        } catch (e: InterruptedException) {
            Log.e(TAG, "Interrupted while waiting", e)
        }

        // 交给默认处理器处理
        defaultHandler?.uncaughtException(thread, throwable)
    }
    
    /**
     * 显示错误提示对话框
     */
    private fun showErrorDialog(throwable: Throwable) {
        try {
            Handler(Looper.getMainLooper()).post {
                val context = context ?: return@post
                
                // 获取简洁的错误信息
                val errorMsg = throwable.message ?: "Unknown error"
                val errorType = throwable.javaClass.simpleName
                
                val dialog = AlertDialog.Builder(context)
                    .setTitle("😵 Galize 发生了错误")
                    .setMessage(
                        "很抱歉，应用遇到了一个问题。\n\n" +
                        "错误类型: $errorType\n" +
                        "错误信息: $errorMsg\n\n" +
                        "崩溃日志已保存到：\n" +
                        "Android/data/com.galize.app/files/crash_logs/\n\n" +
                        "您可以重启应用继续使用。"
                    )
                    .setPositiveButton("确定") { _, _ ->
                        // 不做任何事，让系统继续处理
                    }
                    .setCancelable(false)
                    .create()
                
                // 需要使用 FLAG_ACTIVITY_NEW_TASK 类型的 context
                if (context is Activity) {
                    dialog.window?.setType(
                        android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
                    )
                }
                dialog.show()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show error dialog", e)
        }
    }

    /**
     * 保存崩溃日志到文件
     */
    private fun saveCrashLog(timestamp: String, header: String, throwable: Throwable) {
        if (logDir == null) return

        try {
            val logFile = File(logDir, "crash_$timestamp.log")
            FileWriter(logFile).use { writer ->
                writer.write(header)
                writer.write("\n\n")
                writer.write(GalizeLogger.formatException(throwable))
                writer.write("\n\n")
                writer.write("Device Info:\n")
                writer.write("  Brand: ${Build.BRAND}\n")
                writer.write("  Manufacturer: ${Build.MANUFACTURER}\n")
                writer.write("  Model: ${Build.MODEL}\n")
                writer.write("  SDK: ${Build.VERSION.SDK_INT}\n")
                writer.write("  Release: ${Build.VERSION.RELEASE}\n")
            }
            Log.i(TAG, "Crash log saved to: ${logFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
        }
    }

    /**
     * 获取应用版本号
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context?.packageManager?.getPackageInfo(context?.packageName ?: "", 0)
            "${packageInfo?.versionName} (${packageInfo?.versionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
}
