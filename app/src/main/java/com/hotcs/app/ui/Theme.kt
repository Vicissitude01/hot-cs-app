package com.hotcs.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

// Claude Code 终端风配色
val ClaudeCoral = Color(0xFFD97757)   // 高亮/强调（Claude 珊瑚橙）
val ClaudeBg = Color(0xFF121110)      // 背景（近黑）
val ClaudeSurface = Color(0xFF1B1918) // 面板/输入区
val ClaudeText = Color(0xFFE8E6E3)    // 主文本（暖白）
val ClaudeMuted = Color(0xFF8A8784)   // 次要文本
val ClaudeGreen = Color(0xFF9ECB8E)   // 解读/提示（终端绿）
val ClaudeOutline = Color(0xFF3A3836) // 边框分隔线

val ClaudeColors = darkColorScheme(
    primary = ClaudeCoral,
    onPrimary = Color(0xFF14100E),
    background = ClaudeBg,
    onBackground = ClaudeText,
    surface = ClaudeSurface,
    onSurface = ClaudeText,
    onSurfaceVariant = ClaudeMuted,
    outline = ClaudeOutline,
    secondary = ClaudeGreen,
    onSecondary = Color(0xFF14110E),
)

// 全局等宽字体：material3 的 Typography 没有 defaultFontFamily 参数，
// 用 LocalTextStyle 注入 FontFamily.Monospace（ponytail: 最省事做法）
@Composable
fun ClaudeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ClaudeColors) {
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(fontFamily = FontFamily.Monospace)
        ) {
            content()
        }
    }
}
