package com.hotcs.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hotcs.app.data.HotItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(item: HotItem?, onBack: () -> Unit) {
    val context = LocalContext.current
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        if (item == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("未找到该热点")
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            SourceBadge(item.source)
            Spacer(Modifier.height(10.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "热度 ${item.score}" +
                    if (item.publishedAt.isNotBlank()) " · ${item.publishedAt}" else "",
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("在浏览器打开原文")
            }
            if (item.summary.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Text("AI 解读", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(item.summary)
            }
            if (item.keyPoints.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("要点", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                item.keyPoints.forEach { kp ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("• ", color = MaterialTheme.colorScheme.primary)
                        Text(kp)
                    }
                }
            }
        }
    }
}
