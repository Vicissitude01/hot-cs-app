package com.hotcs.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotcs.app.data.HotRepository
import com.hotcs.app.data.Settings
import com.hotcs.app.notify.Notifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var url by remember { mutableStateOf(Settings.url(context)) }
    var interval by remember { mutableStateOf(Settings.intervalMin(context)) }
    var notify by remember { mutableStateOf(Settings.notifyEnabled(context)) }
    var testMsg by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(ClaudeBg)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                Text("←", color = ClaudeText, fontSize = 15.sp)
            }
            Text("┌─ 设置", color = ClaudeCoral, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        HorizontalDivider(color = ClaudeOutline)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("› 后端地址", color = ClaudeMuted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = ClaudeText),
                placeholder = { Text("https://用户名.github.io/仓库/data/hot.json", color = ClaudeMuted) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = ClaudeCoral,
                    unfocusedBorderColor = ClaudeOutline,
                    focusedTextColor = ClaudeText,
                    unfocusedTextColor = ClaudeText,
                    cursorColor = ClaudeCoral
                )
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "例：https://alice.github.io/hot-cs-app/data/hot.json",
                color = ClaudeMuted,
                fontSize = 11.sp
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, ClaudeOutline),
                color = ClaudeSurface
            ) {
                TextButton(
                    onClick = {
                        Settings.setUrl(context, url.trim())
                        scope.launch {
                            testMsg = runCatching {
                                val n = withContext(Dispatchers.IO) { HotRepository(context).fetch(url.trim()) }
                                "连接成功，拉到 $n 条热点"
                            }.getOrElse { "连接失败：${it.message}" }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("› 保存并测试连接", color = ClaudeText)
                }
            }
            if (testMsg.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(testMsg, color = ClaudeGreen, fontSize = 12.sp)
            }

            Spacer(Modifier.height(22.dp))
            Text("› 刷新间隔（分钟）", color = ClaudeMuted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Row {
                listOf(15L, 20L, 30L, 60L).forEach { m ->
                    val selected = interval == m
                    Text(
                        if (selected) "[ $m ]" else "  $m  ",
                        color = if (selected) ClaudeCoral else ClaudeText,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clickable {
                                interval = m
                                Settings.setIntervalMin(context, m)
                                if (notify) Notifier.schedule(context)
                            }
                            .padding(end = 10.dp)
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        notify = !notify
                        Settings.setNotifyEnabled(context, notify)
                        if (notify) Notifier.schedule(context) else Notifier.cancel(context)
                    }
                    .padding(vertical = 6.dp)
            ) {
                Text(
                    if (notify) "[x]" else "[ ]",
                    color = ClaudeCoral,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(8.dp))
                Text("后台通知", color = ClaudeText, fontSize = 14.sp)
            }

            Spacer(Modifier.height(22.dp))
            Text(
                "提示：改完后端地址和间隔立即生效。通知需在 Android 12+ 的系统设置里允许本应用通知；国产 ROM 可能需在「自启动管理」放行。",
                color = ClaudeMuted,
                fontSize = 11.sp
            )
        }
    }
}
