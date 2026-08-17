package com.hotcs.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import com.hotcs.app.data.HotItem

val sourceNames = mapOf(
    "hackernews" to "HN",
    "github-trending" to "GitHub",
    "juejin" to "掘金",
    "v2ex" to "V2EX",
    "lobsters" to "Lobsters",
)

@Composable
fun HomeScreen(
    items: List<HotItem>,
    loading: Boolean,
    onRefresh: () -> Unit,
    onOpen: (String) -> Unit,
    onSettings: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(ClaudeBg)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("┌─ CS 热点", color = ClaudeCoral, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.weight(1f))
            Text("${items.size} 条", color = ClaudeMuted, fontSize = 12.sp)
        }
        HorizontalDivider(color = ClaudeOutline)

        if (items.isEmpty() && !loading) {
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有数据，先在设置里填后端地址", color = ClaudeMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    TextButton(onClick = onSettings) { Text("[ 去设置 ]", color = ClaudeCoral) }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 2.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    HotRow(
                        item = item,
                        onClick = { onOpen(item.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }

        HorizontalDivider(color = ClaudeOutline)
        Row(
            Modifier
                .fillMaxWidth()
                .background(ClaudeSurface)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(">", color = ClaudeCoral, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(10.dp))
            TextButton(onClick = onSettings) { Text("[ 设置 ]", color = ClaudeText, fontSize = 13.sp) }
            TextButton(onClick = onRefresh) {
                Text(if (loading) "[ 刷新中… ]" else "[ 刷新 ]", color = ClaudeText, fontSize = 13.sp)
            }
            Spacer(Modifier.weight(1f))
            Text("hot-cs", color = ClaudeMuted, fontSize = 11.sp)
        }
    }
}

@Composable
fun HotRow(item: HotItem, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
    Column(
        modifier
            .fillMaxWidth()
            .scale(scale)
            .clickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("[${sourceNames[item.source] ?: item.source}]", color = ClaudeCoral, fontSize = 12.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                item.title,
                color = ClaudeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Text("(${item.score})", color = ClaudeMuted, fontSize = 12.sp)
        }
        if (item.summary.isNotBlank()) {
            Spacer(Modifier.height(5.dp))
            Text(
                "  └ " + item.summary,
                color = ClaudeGreen,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(color = ClaudeOutline.copy(alpha = 0.6f))
}
