package com.galize.app.utils

import android.util.Log

/**
 * 统一日志工具类
 * 支持自定义 Tag，便于按模块过滤日志
 * 
 * 使用方式:
 * - 默认 Tag: GalizeLogger().I("message") - 使用 "Galize" Tag
 * - 自定义 Tag: GalizeLogger("ModuleName").I("message") - 使用自定义 Tag
 * 
 * 日志级别:
 * - D(): Debug - 调试信息
 * - I(): Info - 重要流程节点
 * - W(): Warning - 潜在问题
 * - E(): Error - 错误和异常
 * - V(): Verbose - 详细信息
 */
class GalizeLogger(private val tag: String = "Galize") {
    
    companion object {
        private const val DEFAULT_TAG = "Galize"
    }

    fun D(message: String) {
        logLong(Log.DEBUG, message)
    }

    fun I(message: String) {
        logLong(Log.INFO, message)
    }

    fun W(message: String, throwable: Throwable? = null) {
        Log.w(tag, message, throwable)
    }

    fun E(message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
    }

    fun V(message: String) {
        logLong(Log.VERBOSE, message)
    }

    /**
     * 分段打印长日志，避免 Logcat 4000 字符截断
     */
    private fun logLong(level: Int, message: String, maxLen: Int = 3800) {
        if (message.length <= maxLen) {
            Log.println(level, tag, message)
            return
        }
        var start = 0
        var part = 1
        while (start < message.length) {
            val end = minOf(start + maxLen, message.length)
            Log.println(level, tag, "[Part $part] ${message.substring(start, end)}")
            start = end
            part++
        }
    }

    /**
     * 格式化异常信息，方便阅读
     */
    fun formatException(throwable: Throwable): String {
        val sb = StringBuilder()
        sb.append("Exception: ${throwable.javaClass.simpleName}\n")
        sb.append("Message: ${throwable.message}\n")
        sb.append("Stack trace:\n")
        throwable.stackTrace.forEach { element ->
            sb.append("  at ${element.className}.${element.methodName}(${element.fileName}:${element.lineNumber})\n")
        }
        if (throwable.cause != null) {
            sb.append("\nCaused by:\n")
            sb.append(formatException(throwable.cause!!))
        }
        return sb.toString()
    }
}
