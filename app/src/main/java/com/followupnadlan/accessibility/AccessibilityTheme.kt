package com.followupnadlan.accessibility

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.followupnadlan.R

/**
 * Calm accessibility palette, re-skinned from the Claude Design prototype (FollowUp.dc.html,
 * tokens in followup-design-tokens.md). Field names kept stable so the ~280 existing call sites
 * across the app don't need to change — only the values moved to match the design tokens.
 * Light values are the source of truth; *Dark values are our own derivation (prototype is
 * light-only).
 */
object AccessibilityColors {
    // Light
    val ScreenBackground = Color(0xFFF7F9FA)
    val ScreenBackgroundAlt = Color(0xFFF7F9FA)
    val Surface = Color(0xFFFFFFFF)
    val SubtleSurface = Color(0xFFF8F9FB)
    val CardBorder = Color(0xFFEEF3F9)

    val Primary = Color(0xFF3F8CFF)
    val PrimaryContainer = Color(0xFFEEF3F9)
    val PrimaryTint = Color(0xFFEEF3F9)
    val PrimaryTintBorder = Color(0xFFD8E6F8)

    val Green = Color(0xFF17B3A3)
    val GreenBright = Color(0xFF17B3A3)
    val GreenCheck = Color(0xFF34B7F1)
    val GreenContainer = Color(0xFFE1F7CB)
    val GreenSurface = Color(0xFFE1F7CB)

    val Heading = Color(0xFF111B21)
    val TextStrong = Color(0xFF111B21)
    val TextBody = Color(0xFF3B4A54)
    val TextMuted = Color(0xFF667781)
    val TextFaint = Color(0xFF8A97A0)

    val Danger = Color(0xFFE6483C)
    val Warning = Color(0xFFE6483C)
    val UnselectedIcon = Color(0xFF8A97A0)
    val IconDark = Color(0xFF16241B)

    // New tokens not previously named (segmented control / field backgrounds / list rows)
    val FieldGrey = Color(0xFFF2F4F7)
    val ListBg = Color(0xFFF8F9FB)
    val PickerBg = Color(0xFFEEF1F4)
    val BubbleGreen = Color(0xFFE1F7CB)
    val WaCheck = Color(0xFF34B7F1)
    val DangerBg = Color(0xFFFFE0D6)

    // Dark (derived — not in prototype)
    val ScreenBackgroundDark = Color(0xFF141B16)
    val ScreenBackgroundAltDark = Color(0xFF1B2630)
    val SurfaceDark = Color(0xFF141B16)
    val SubtleSurfaceDark = Color(0xFF1A211D)
    val CardBorderDark = Color(0xFF1B2630)

    val PrimaryDark = Color(0xFF5B9EFF)
    val PrimaryContainerDark = Color(0xFF1B2630)

    val GreenDark = Color(0xFF2BC4B3)
    val GreenContainerDark = Color(0xFF244025)

    val HeadingDark = Color(0xFFE8EFE9)
    val TextStrongDark = Color(0xFFE8EFE9)
    val TextBodyDark = Color(0xB3E8EFE9)
    val TextMutedDark = Color(0x8CE8EFE9)
    val TextFaintDark = Color(0x66E8EFE9)

    val DangerDark = Color(0xFFFF6B5C)
    val FieldGreyDark = Color(0xFF20272C)
    val ListBgDark = Color(0xFF1A211D)
    val PickerBgDark = Color(0xFF1F272B)
    val BubbleGreenDark = Color(0xFF244025)
    val DangerBgDark = Color(0xFF3A241F)
}

/**
 * Palette snapshot for the current light/dark mode, exposed via CompositionLocal so screens can
 * read the correct variant without branching on `isSystemInDarkTheme()` themselves. Prefer these
 * over the static `AccessibilityColors.X` fields in new code (§4 dark-mode / danger-row work);
 * old call sites keep working unchanged against the static light-biased values above.
 */
data class AccessibilityExtendedColors(
    val screenBackground: Color,
    val screenBackgroundAlt: Color,
    val surface: Color,
    val subtleSurface: Color,
    val cardBorder: Color,
    val primary: Color,
    val primaryContainer: Color,
    val green: Color,
    val greenContainer: Color,
    val heading: Color,
    val textStrong: Color,
    val textBody: Color,
    val textMuted: Color,
    val textFaint: Color,
    val danger: Color,
    val fieldGrey: Color,
    val listBg: Color,
    val pickerBg: Color,
    val bubbleGreen: Color,
    val waCheck: Color,
    val dangerBg: Color
)

private val LightExtendedColors = AccessibilityExtendedColors(
    screenBackground = AccessibilityColors.ScreenBackground,
    screenBackgroundAlt = AccessibilityColors.ScreenBackgroundAlt,
    surface = AccessibilityColors.Surface,
    subtleSurface = AccessibilityColors.SubtleSurface,
    cardBorder = AccessibilityColors.CardBorder,
    primary = AccessibilityColors.Primary,
    primaryContainer = AccessibilityColors.PrimaryContainer,
    green = AccessibilityColors.Green,
    greenContainer = AccessibilityColors.GreenContainer,
    heading = AccessibilityColors.Heading,
    textStrong = AccessibilityColors.TextStrong,
    textBody = AccessibilityColors.TextBody,
    textMuted = AccessibilityColors.TextMuted,
    textFaint = AccessibilityColors.TextFaint,
    danger = AccessibilityColors.Danger,
    fieldGrey = AccessibilityColors.FieldGrey,
    listBg = AccessibilityColors.ListBg,
    pickerBg = AccessibilityColors.PickerBg,
    bubbleGreen = AccessibilityColors.BubbleGreen,
    waCheck = AccessibilityColors.WaCheck,
    dangerBg = AccessibilityColors.DangerBg
)

private val DarkExtendedColors = AccessibilityExtendedColors(
    screenBackground = AccessibilityColors.ScreenBackgroundDark,
    screenBackgroundAlt = AccessibilityColors.ScreenBackgroundAltDark,
    surface = AccessibilityColors.SurfaceDark,
    subtleSurface = AccessibilityColors.SubtleSurfaceDark,
    cardBorder = AccessibilityColors.CardBorderDark,
    primary = AccessibilityColors.PrimaryDark,
    primaryContainer = AccessibilityColors.PrimaryContainerDark,
    green = AccessibilityColors.GreenDark,
    greenContainer = AccessibilityColors.GreenContainerDark,
    heading = AccessibilityColors.HeadingDark,
    textStrong = AccessibilityColors.TextStrongDark,
    textBody = AccessibilityColors.TextBodyDark,
    textMuted = AccessibilityColors.TextMutedDark,
    textFaint = AccessibilityColors.TextFaintDark,
    danger = AccessibilityColors.DangerDark,
    fieldGrey = AccessibilityColors.FieldGreyDark,
    listBg = AccessibilityColors.ListBgDark,
    pickerBg = AccessibilityColors.PickerBgDark,
    bubbleGreen = AccessibilityColors.BubbleGreenDark,
    waCheck = AccessibilityColors.WaCheck,
    dangerBg = AccessibilityColors.DangerBgDark
)

val LocalAccessibilityColors = staticCompositionLocalOf { LightExtendedColors }

/** Convenience accessor: `AccessibilityExtra.colors.bubbleGreen` from inside a `@Composable`. */
object AccessibilityExtra {
    val colors: AccessibilityExtendedColors
        @Composable get() = LocalAccessibilityColors.current
}

private val LightColorScheme = lightColorScheme(
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

private val DarkColorScheme = darkColorScheme(
    primary = AccessibilityColors.PrimaryDark,
    onPrimary = Color.White,
    primaryContainer = AccessibilityColors.PrimaryContainerDark,
    secondary = AccessibilityColors.GreenDark,
    onSecondary = Color.White,
    background = AccessibilityColors.ScreenBackgroundDark,
    onBackground = AccessibilityColors.TextStrongDark,
    surface = AccessibilityColors.SurfaceDark,
    onSurface = AccessibilityColors.TextStrongDark,
    surfaceVariant = AccessibilityColors.SubtleSurfaceDark,
    onSurfaceVariant = AccessibilityColors.TextMutedDark,
    error = AccessibilityColors.DangerDark,
    onError = Color.White
)

// Heebo (Google Fonts, OFL) bundled as a variable font asset — falls back to system default
// if the resource is ever missing since FontFamily.Default remains the last resort.
private val HeeboFontFamily = FontFamily(
    Font(R.font.heebo_variable, weight = FontWeight.Normal),
    Font(R.font.heebo_variable, weight = FontWeight.Medium),
    Font(R.font.heebo_variable, weight = FontWeight.SemiBold),
    Font(R.font.heebo_variable, weight = FontWeight.Bold),
    Font(R.font.heebo_variable, weight = FontWeight.ExtraBold)
)

private fun accessibilityTypography(fontFamily: FontFamily) = Typography(
    headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 24.sp),
    titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, lineHeight = 22.sp),
    bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 17.sp, lineHeight = 26.sp),
    bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp),
    labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 20.sp)
)

private val AccessibilityShapes = Shapes(
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp)
)

@Composable
fun AccessibilityTheme(content: @Composable () -> Unit) {
    // MVP-1: light only, regardless of system setting — dark mode is MVP-2.
    val dark = false
    val colorScheme = if (dark) DarkColorScheme else LightColorScheme
    val extendedColors = if (dark) DarkExtendedColors else LightExtendedColors

    androidx.compose.runtime.CompositionLocalProvider(LocalAccessibilityColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = accessibilityTypography(HeeboFontFamily),
            shapes = AccessibilityShapes,
            content = content
        )
    }
}
