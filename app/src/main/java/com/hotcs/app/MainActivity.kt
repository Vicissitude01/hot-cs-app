package com.hotcs.app

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hotcs.app.data.HotRepository
import com.hotcs.app.data.Settings
import com.hotcs.app.notify.Notifier
import com.hotcs.app.ui.ClaudeTheme
import com.hotcs.app.ui.DetailScreen
import com.hotcs.app.ui.HomeScreen
import com.hotcs.app.ui.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Notifier.ensureChannel(this)
        if (Settings.notifyEnabled(this)) Notifier.schedule(this)
        requestNotifyPermission()
        setContent {
            ClaudeTheme {
                App()
            }
        }
    }

    private fun requestNotifyPermission() {
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
fun App() {
    val context = LocalContext.current
    val repo = remember { HotRepository(context) }
    var items by remember { mutableStateOf(repo.cached()) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val refresh: () -> Unit = {
        if (!loading) {
            loading = true
            scope.launch {
                val url = Settings.url(context)
                if (url.isNotBlank()) {
                    runCatching { withContext(Dispatchers.IO) { repo.fetch(url) } }
                        .onSuccess { items = it }
                }
                loading = false
            }
        }
    }

    val nav = rememberNavController()
    val activity = LocalContext.current as? Activity
    var pendingId by remember { mutableStateOf(activity?.intent?.getStringExtra("itemId")) }

    // 点通知进入：等列表加载出目标条目后跳详情
    LaunchedEffect(pendingId, items) {
        val id = pendingId
        if (id != null && items.any { it.id == id }) {
            nav.navigate("detail/$id")
            pendingId = null
        }
    }

    NavHost(
        nav,
        startDestination = "home",
        enterTransition = { fadeIn(tween(250)) + slideInHorizontally(tween(300)) { it / 12 } },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(250)) },
        popExitTransition = { fadeOut(tween(200)) + slideOutHorizontally(tween(300)) { it / 12 } }
    ) {
        composable("home") {
            HomeScreen(
                items = items,
                loading = loading,
                onRefresh = refresh,
                onOpen = { id -> nav.navigate("detail/$id") },
                onSettings = { nav.navigate("settings") }
            )
        }
        composable("detail/{id}") { back ->
            val id = back.arguments?.getString("id") ?: ""
            DetailScreen(item = items.find { it.id == id }, onBack = { nav.popBackStack() })
        }
        composable("settings") {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
