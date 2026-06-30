package com.followupnadlan.accessibility

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Calm accessibility palette taken from the Claude Design "Accessibility Screens" file.
 * Light background, soft blue / green accents, dark readable text. This replaces the old
 * FollowUp business styling so the app reads as a quiet accessibility tool, not a CRM.
 */
object AccessibilityColors {
    val ScreenBackground = Color(0xFFEEF3F8)
    val ScreenBackgroundAlt = Color(0xFFE6ECF3)
    val Surface = Color(0xFFFFFFFF)
    val SubtleSurface = Color(0xFFF6F9FC)
    val CardBorder = Color(0xFFE7EEF5)

    val Primary = Color(0xFF2F6FE0)
    val PrimaryContainer = Color(0xFFEAF1FE)
    val PrimaryTint = Color(0xFFEEF4FC)
    val PrimaryTintBorder = Color(0xFFD8E6F8)

    val Green = Color(0xFF159A5B)
    val GreenBright = Color(0xFF25A35A)
    val GreenCheck = Color(0xFF2FAA67)
    val GreenContainer = Color(0xFFE7F5EC)
    val GreenSurface = Color(0xFFEAF6EE)

    val Heading = Color(0xFF16365E)
    val TextStrong = Color(0xFF1F2B40)
    val TextBody = Color(0xFF46566F)
    val TextMuted = Color(0xFF5A6B85)
    val TextFaint = Color(0xFF8392A8)

    val Danger = Color(0xFFE0483D)
    val Warning = Color(0xFFE89327)
    val UnselectedIcon = Color(0xFFB6C1D1)
    val IconDark = Color(0xFF33415C)
}

private val AccessibilityColorScheme = lightColorScheme(
    primary = AccessibilityColors.Primary,
    onPrimary = Color.White,
    primaryContainer = AccessibilityColors.PrimaryContainer,
    secondary = AccessibilityColors.Green,
    onSecondary = Color.White,
    background = AccessibilityColors.ScreenBackground,
    onBackground = AccessibilityColors.TextStrong,
    surface = AccessibilityColors.Surface,
    onSurface = AccessibilityColors.TextStrong,
    surfaceVariant = AccessibilityColors.SubtleSurface,
    onSurfaceVariant = AccessibilityColors.TextMuted,
    error = AccessibilityColors.Danger,
    onError = Color.White
)

private val AccessibilityTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 23.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 16.sp, lineHeight = 22.sp)
)

private val AccessibilityShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun AccessibilityTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AccessibilityColorScheme,
        typography = AccessibilityTypography,
        shapes = AccessibilityShapes,
        content = content
    )
}
