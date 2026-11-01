package com.luna.oneuilauncher

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.luna.oneuilauncher.ui.HomeScreen
import com.luna.oneuilauncher.ui.theme.OneUILauncherTheme
import com.luna.oneuilauncher.util.CrashHandler
import com.luna.oneuilauncher.viewmodel.LauncherViewModel

class OneUILauncherApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OneUILauncherTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val vm: LauncherViewModel = viewModel(
                        factory = LauncherViewModel.Factory(application)
                    )
                    HomeScreen(viewModel = vm)
                }
            }
        }
    }
}
