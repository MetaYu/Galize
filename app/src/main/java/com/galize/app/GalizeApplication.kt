package com.galize.app

import android.app.Application
import com.galize.app.utils.CrashHandler
import com.galize.app.utils.GalizeLogger
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GalizeApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // 初始化全局异常捕获器
        CrashHandler.getInstance().init(this)
        
        GalizeLogger.i("GalizeApplication initialized")
    }
}
