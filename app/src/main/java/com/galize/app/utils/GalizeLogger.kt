package com.galize.app.utils

import android.util.Log

/**
 * 统一日志工具类
 * 所有日志都使用 "Galize" 作为 TAG，方便过滤
 */
object GalizeLogger {
    private const val TAG = "Galize"

    fun d(message: String, throwable: Throwable? = null) {
        Log.d(TAG, message, throwable)
    }

    fun i(message: String, throwable: Throwable? = null) {
        Log.i(TAG, message, throwable)
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, message, throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, message, throwable)
    }

    fun v(message: String, throwable: Throwable? = null) {
        Log.v(TAG, message, throwable)
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
