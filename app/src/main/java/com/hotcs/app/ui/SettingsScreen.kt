package com.hotcs.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.hotcs.app.data.HotRepository
import com.hotcs.app.data.Settings
import com.hotcs.app.notify.Notifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(Settings.url(context)) }
    var interval by remember { mutableStateOf(Settings.intervalMin(context)) }
    var notify by remember { mutableStateOf(Settings.notifyEnabled(context)) }
    var testMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("后端地址（GitHub Pages 上的 hot.json）", style = MaterialTheme.typography.titleSmall)
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("https://用户名.github.io/仓库/data/hot.json") }
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "示例：https://alice.github.io/hot-cs-app/data/hot.json",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = {
                Settings.setUrl(context, url.trim())
                scope.launch {
                    testMsg = runCatching {
                        val n = HotRepository(context).fetch(url.trim())
                        "连接成功，拉到 $n 条热点"
                    }.getOrElse { "连接失败：${it.message}" }
                }
            }) {
                Text("保存并测试连接")
            }
            if (testMsg.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(testMsg, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(20.dp))
            Text("刷新间隔（分钟）", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(6.dp))
            Row {
                listOf(15L, 20L, 30L, 60L).forEach { m ->
                    FilterChip(
                        selected = interval == m,
                        onClick = { interval = m; Settings.setIntervalMin(context, m) },
                        label = { Text("$m") },
                        modifier = Modifier.padding(end = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("后台通知", modifier = Modifier.weight(1f))
                Switch(checked = notify, onCheckedChange = { v ->
                    notify = v
                    Settings.setNotifyEnabled(context, v)
                    if (v) Notifier.schedule(context) else Notifier.cancel(context)
                })
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "提示：改完后端地址和间隔立即生效。通知需在 Android 12+ 的系统设置里允许本应用通知。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
