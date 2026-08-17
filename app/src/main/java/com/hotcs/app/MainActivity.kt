package com.hotcs.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hotcs.app.data.HotItem
import com.hotcs.app.data.HotRepository
import com.hotcs.app.data.Settings
import com.hotcs.app.ui.DetailScreen
import com.hotcs.app.ui.HomeScreen
import com.hotcs.app.ui.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { App() }
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
                    runCatching { repo.fetch(url) }.onSuccess { items = it }
                }
                loading = false
            }
        }
    }

    val nav = rememberNavController()
    NavHost(nav, startDestination = "home") {
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
