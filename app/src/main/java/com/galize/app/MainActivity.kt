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
        GalizeLogger("MainActivity").I("onCreate")
        
        enableEdgeToEdge()
        setContent {
            GalizeTheme {
                GalizeNavGraph()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        GalizeLogger("MainActivity").I("onStart")
    }

    override fun onResume() {
        super.onResume()
        GalizeLogger("MainActivity").I("onResume")
    }

    override fun onPause() {
        super.onPause()
        GalizeLogger("MainActivity").I("onPause")
    }

    override fun onStop() {
        super.onStop()
        GalizeLogger("MainActivity").I("onStop")
    }

    override fun onDestroy() {
        super.onDestroy()
        GalizeLogger("MainActivity").I("onDestroy")
    }
}
