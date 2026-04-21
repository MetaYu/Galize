package com.galize.app.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import com.galize.app.utils.GalizeLogger

/**
 * Manages screen capture permissions and intent creation.
 * 
 * Usage flow:
 * 1. Call [createScreenCaptureIntent] to get the permission intent
 * 2. Launch it with startActivityForResult from Activity
 * 3. On result OK, create [ScreenCaptureManager] with resultCode and data
 */
class ScreenCapturePermissionManager(
    private val context: Context
) {
    private val logger = GalizeLogger("ScreenCapturePermission")
    
    private val projectionManager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) 
        as MediaProjectionManager

    /**
     * Creates an intent for requesting screen capture permission.
     * This intent should be launched with startActivityForResult.
     * 
     * @return Intent to start for permission request
     */
    fun createScreenCaptureIntent(): Intent {
        logger.D("Creating screen capture intent")
        return projectionManager.createScreenCaptureIntent()
    }

    /**
     * Checks if the app currently has screen capture permission.
     * Note: This only checks if we had permission before, not if it's still valid.
     * The permission can be revoked by system at any time.
     */
    fun hasPermission(): Boolean {
        // MediaProjection permission is transient and must be requested each session
        // We can't reliably check if we have it without trying to use it
        return false
    }

    companion object {
        const val REQUEST_CODE_SCREEN_CAPTURE = 1001
        
        /**
         * Validates the result from screen capture permission request.
         * 
         * @param requestCode The request code from onActivityResult
         * @param resultCode The result code from onActivityResult
         * @param data The intent data from onActivityResult
         * @return true if permission was granted, false otherwise
         */
        fun handleResult(
            requestCode: Int,
            resultCode: Int,
            data: Intent?
        ): Boolean {
            if (requestCode != REQUEST_CODE_SCREEN_CAPTURE) {
                return false
            }
            
            if (resultCode == Activity.RESULT_OK && data != null) {
                GalizeLogger("ScreenCapturePermission").I("Screen capture permission granted")
                return true
            } else {
                GalizeLogger("ScreenCapturePermission").W("Screen capture permission denied")
                return false
            }
        }
    }
}
