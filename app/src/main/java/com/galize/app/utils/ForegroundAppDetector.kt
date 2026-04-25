package com.galize.app.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import com.galize.app.model.AppType

/**
 * Utility class to detect the current foreground application.
 * Used to identify which chat app the user is currently viewing.
 */
class ForegroundAppDetector(private val context: Context) {
    private val logger = GalizeLogger("ForegroundAppDetector")

    /**
     * Gets the package name of the current foreground app.
     * 
     * Note: Starting from Android 5.0 (Lollipop), getRunningTasks() is deprecated
     * and only returns the caller's own tasks for privacy reasons.
     * This method works for Android 5.0+ but requires special permissions
     * or usage stats access for accurate results.
     */
    fun getForegroundPackageName(): String {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // For Android 5.0+, we need to use UsageStatsManager
                // But this requires special permission, so we'll use a fallback
                val runningTasks = activityManager.getRunningTasks(1)
                if (runningTasks.isNotEmpty()) {
                    val packageName = runningTasks[0].topActivity?.packageName ?: ""
                    logger.D("Foreground package: $packageName")
                    packageName
                } else {
                    logger.W("No running tasks found")
                    ""
                }
            } else {
                // For older Android versions
                val runningTasks = activityManager.getRunningTasks(1)
                if (runningTasks.isNotEmpty()) {
                    runningTasks[0].topActivity?.packageName ?: ""
                } else {
                    ""
                }
            }
        } catch (e: Exception) {
            logger.E("Failed to get foreground package: ${e.message}", e)
            ""
        }
    }

    /**
     * Determines the app type based on package name.
     */
    fun detectAppType(packageName: String): AppType {
        return when {
            packageName.contains("com.tencent.mm") -> AppType.WECHAT
            packageName.contains("com.tencent.mobileqq") -> AppType.QQ
            packageName.contains("com.tencent.qqlite") -> AppType.QQ
            packageName.contains("com.soul") -> AppType.DATING_APP
            packageName.contains("com.momo") -> AppType.DATING_APP
            packageName.contains("com.tantan") -> AppType.DATING_APP
            else -> AppType.GENERIC
        }
    }

    /**
     * Gets the user-friendly app name from package name.
     */
    fun getAppName(packageName: String): String {
        return when {
            packageName.contains("com.tencent.mm") -> "微信"
            packageName.contains("com.tencent.mobileqq") -> "QQ"
            packageName.contains("com.tencent.qqlite") -> "QQ轻聊版"
            packageName.contains("com.soul") -> "Soul"
            packageName.contains("com.momo") -> "陌陌"
            packageName.contains("com.tantan") -> "探探"
            else -> packageName.takeLastWhile { it != '.' }.replaceFirstChar { it.uppercase() }
        }
    }
}
