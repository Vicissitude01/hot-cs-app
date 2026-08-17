package com.hotcs.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hotcs.app.data.HotItem

@Composable
fun DetailScreen(item: HotItem?, onBack: () -> Unit) {
    val context = LocalContext.current
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
            Text("┌─ 详情", color = ClaudeCoral, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        HorizontalDivider(color = ClaudeOutline)

        if (item == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("未找到该热点", color = ClaudeMuted)
            }
            return@Column
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("[${sourceNames[item.source] ?: item.source}]", color = ClaudeCoral, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                Text("(${item.score})", color = ClaudeMuted, fontSize = 12.sp)
                Spacer(Modifier.width(8.dp))
                if (item.publishedAt.isNotBlank()) {
                    Text(item.publishedAt, color = ClaudeMuted, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                item.title,
                color = ClaudeText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, ClaudeOutline),
                color = ClaudeSurface
            ) {
                TextButton(
                    onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(item.url))) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("› 在浏览器打开原文", color = ClaudeText)
                }
            }

            if (item.summary.isNotBlank()) {
                Spacer(Modifier.height(20.dp))
                Text("┌ 解读", color = ClaudeCoral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, ClaudeOutline),
                    color = ClaudeSurface
                ) {
                    Text(
                        item.summary,
                        color = ClaudeGreen,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            if (item.keyPoints.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("┌ 要点", color = ClaudeCoral, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                item.keyPoints.forEach { kp ->
                    Text("•  $kp", color = ClaudeText, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
        }
    }
}
