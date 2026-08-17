package com.hotcs.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
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

// 全局等宽字体：把 material3 的每个文字级别都替换成等宽字体（保留原始字号/字重）
private val baseType = Typography()
val ClaudeTypography = Typography(
    displayLarge = baseType.displayLarge.copy(fontFamily = FontFamily.Monospace),
    displayMedium = baseType.displayMedium.copy(fontFamily = FontFamily.Monospace),
    displaySmall = baseType.displaySmall.copy(fontFamily = FontFamily.Monospace),
    headlineLarge = baseType.headlineLarge.copy(fontFamily = FontFamily.Monospace),
    headlineMedium = baseType.headlineMedium.copy(fontFamily = FontFamily.Monospace),
    headlineSmall = baseType.headlineSmall.copy(fontFamily = FontFamily.Monospace),
    titleLarge = baseType.titleLarge.copy(fontFamily = FontFamily.Monospace),
    titleMedium = baseType.titleMedium.copy(fontFamily = FontFamily.Monospace),
    titleSmall = baseType.titleSmall.copy(fontFamily = FontFamily.Monospace),
    bodyLarge = baseType.bodyLarge.copy(fontFamily = FontFamily.Monospace),
    bodyMedium = baseType.bodyMedium.copy(fontFamily = FontFamily.Monospace),
    bodySmall = baseType.bodySmall.copy(fontFamily = FontFamily.Monospace),
    labelLarge = baseType.labelLarge.copy(fontFamily = FontFamily.Monospace),
    labelMedium = baseType.labelMedium.copy(fontFamily = FontFamily.Monospace),
    labelSmall = baseType.labelSmall.copy(fontFamily = FontFamily.Monospace),
)

@Composable
fun ClaudeTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ClaudeColors, typography = ClaudeTypography) {
        content()
    }
}
