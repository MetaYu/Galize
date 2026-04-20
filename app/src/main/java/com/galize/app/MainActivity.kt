package com.galize.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.galize.app.ui.navigation.GalizeNavGraph
import com.galize.app.ui.theme.GalizeTheme
import com.galize.app.utils.GalizeLogger
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        GalizeLogger.i("MainActivity onCreate")
        
        enableEdgeToEdge()
        setContent {
            GalizeTheme {
                GalizeNavGraph()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        GalizeLogger.i("MainActivity onStart")
    }

    override fun onResume() {
        super.onResume()
        GalizeLogger.i("MainActivity onResume")
    }

    override fun onPause() {
        super.onPause()
        GalizeLogger.i("MainActivity onPause")
    }

    override fun onStop() {
        super.onStop()
        GalizeLogger.i("MainActivity onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        GalizeLogger.i("MainActivity onDestroy")
    }
}
