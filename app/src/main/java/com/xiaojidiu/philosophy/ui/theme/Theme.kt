package com.xiaojidiu.philosophy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

private val ThoughtTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 38.sp, fontWeight = FontWeight.Bold),
    headlineMedium = TextStyle(fontSize = 25.sp, lineHeight = 35.sp, fontWeight = FontWeight.Bold),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 29.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 27.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 23.sp),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium)
)

private val Light = lightColorScheme(primary = Color(0xFF6654D5), secondary = Color(0xFF73C7F5), tertiary = Color(0xFFFFD966), background = Color(0xFFFAFAFC), surface = Color(0xFFFAFAFC), surfaceContainerLow = Color(0xFFF0EEF6), surfaceContainer = Color(0xFFECE9F3), surfaceContainerHigh = Color(0xFFE5E1EF), onSurface = Color(0xFF24222B), onSurfaceVariant = Color(0xFF65616F), primaryContainer = Color(0xFFE7E2FF))
private val Dark = darkColorScheme(primary = Color(0xFFB5A9FF), secondary = Color(0xFF73C7F5), tertiary = Color(0xFFFFD966), background = Color(0xFF1B1D25), surface = Color(0xFF22242E), surfaceContainerLow = Color(0xFF2B2D39), surfaceContainer = Color(0xFF303340), surfaceContainerHigh = Color(0xFF383B49), onSurface = Color(0xFFF4EFFA), onSurfaceVariant = Color(0xFFC9C1D2), primaryContainer = Color(0xFF44396A))
@Composable fun LittleThoughtsTheme(mode: String = "system", content: @Composable () -> Unit) {
    val dark = mode == "dark" || mode == "system" && isSystemInDarkTheme()
    MaterialTheme(colorScheme = if (dark) Dark else Light, typography = ThoughtTypography, shapes = Shapes(small = RoundedCornerShape(14.dp), medium = RoundedCornerShape(22.dp), large = RoundedCornerShape(28.dp)), content = content)
}
