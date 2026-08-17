package com.hotcs.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hotcs.app.data.HotItem

val sourceColors = mapOf(
    "hackernews" to Color(0xFFF0652F),
    "github-trending" to Color(0xFF24292E),
    "juejin" to Color(0xFF1E80FF),
    "v2ex" to Color(0xFF52616B),
    "lobsters" to Color(0xFFAC130D),
)
val sourceNames = mapOf(
    "hackernews" to "HN",
    "github-trending" to "GitHub",
    "juejin" to "掘金",
    "v2ex" to "V2EX",
    "lobsters" to "Lobsters",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    items: List<HotItem>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onOpen: (String) -> Unit,
    onSettings: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CS 热点") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        if (items.isEmpty() && !loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有数据，先在设置里填后端地址")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onSettings) { Text("去设置") }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRefresh) { Text("刷新") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp)
            ) {
                if (loading) {
                    item { LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 6.dp)) }
                }
                items(items, key = { it.id }) { item ->
                    HotCard(item = item, onClick = { onOpen(item.id) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotCard(item: HotItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SourceBadge(item.source)
                Spacer(Modifier.width(8.dp))
                Text("热度 ${item.score}", style = MaterialTheme.typography.labelSmall)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            if (item.summary.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    item.summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SourceBadge(source: String) {
    val name = sourceNames[source] ?: source
    val color = sourceColors[source] ?: Color(0xFF757575)
    Box(
        Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(name, color = Color.White, style = MaterialTheme.typography.labelSmall)
    }
}
