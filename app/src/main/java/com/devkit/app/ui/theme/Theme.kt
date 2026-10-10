package com.devkit.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devkit.app.data.prefs.AccentChoice

val DevKitShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/** Semantic colors that Material 3 does not model directly. */
data class DevKitSemanticColors(
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val info: Color,
    val addedLine: Color,
    val removedLine: Color,
    val codeSurface: Color,
    val codeBorder: Color,
)

val LocalSemanticColors = staticCompositionLocalOf {
    DevKitSemanticColors(
        success = SuccessGreen,
        onSuccess = Color.White,
        warning = WarningAmber,
        info = InfoBlue,
        addedLine = Color(0xFF1B3A2A),
        removedLine = Color(0xFF3A1B1B),
        codeSurface = Navy700,
        codeBorder = Navy500,
    )
}

private fun accentColor(accent: AccentChoice): Color = when (accent) {
    AccentChoice.VIOLET -> Violet500
    AccentChoice.BLUE -> AccentBlue
    AccentChoice.TEAL -> AccentTeal
    AccentChoice.AMBER -> AccentAmber
    AccentChoice.ROSE -> AccentRose
    AccentChoice.GREEN -> AccentGreen
}

fun darkColorSchemeFor(accent: AccentChoice): androidx.compose.material3.ColorScheme {
    val primary = accentColor(accent)
    return darkColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.22f),
        onPrimaryContainer = Violet50,
        secondary = Violet400,
        onSecondary = Navy900,
        secondaryContainer = Navy600,
        onSecondaryContainer = Violet200,
        tertiary = Violet400,
        onTertiary = Navy900,
        background = Navy900,
        onBackground = Color(0xFFE6E8F2),
        surface = Navy800,
        onSurface = Color(0xFFE6E8F2),
        surfaceVariant = Navy700,
        onSurfaceVariant = Color(0xFFB6BAD3),
        surfaceContainer = Navy700,
        surfaceContainerHigh = Navy600,
        surfaceContainerLow = Navy800,
        outline = Navy500,
        outlineVariant = Navy600,
        error = ErrorRedDark,
        onError = Color.White,
        scrim = Color(0xCC000000),
    )
}

fun lightColorSchemeFor(accent: AccentChoice): androidx.compose.material3.ColorScheme {
    val primary = accentColor(accent)
    return lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primary.copy(alpha = 0.14f),
        onPrimaryContainer = primary,
        secondary = Violet600,
        onSecondary = Color.White,
        secondaryContainer = Violet50,
        onSecondaryContainer = Violet600,
        tertiary = Violet600,
        onTertiary = Color.White,
        background = SurfaceLight,
        onBackground = Color(0xFF1A1C2B),
        surface = SurfaceCardLight,
        onSurface = Color(0xFF1A1C2B),
        surfaceVariant = Color(0xFFF1F2F8),
        onSurfaceVariant = Color(0xFF535A78),
        surfaceContainer = Color(0xFFF6F7FB),
        surfaceContainerHigh = Color(0xFFEFF1F7),
        surfaceContainerLow = Color(0xFFFCFCFF),
        outline = OutlineLight,
        outlineVariant = Color(0xFFE7E9F1),
        error = ErrorRed,
        onError = Color.White,
        scrim = Color(0x66000000),
    )
}

fun darkSemanticColors() = DevKitSemanticColors(
    success = SuccessGreenDark,
    onSuccess = Navy900,
    warning = WarningAmberDark,
    info = InfoBlueDark,
    addedLine = Color(0xFF16301F),
    removedLine = Color(0xFF33191A),
    codeSurface = Navy700,
    codeBorder = Navy500,
)

fun lightSemanticColors() = DevKitSemanticColors(
    success = SuccessGreen,
    onSuccess = Color.White,
    warning = WarningAmber,
    info = InfoBlue,
    addedLine = Color(0xFFE1F5E9),
    removedLine = Color(0xFFFBE3E3),
    codeSurface = Color(0xFFF4F5FA),
    codeBorder = Color(0xFFDFE2EC),
)

@Composable
fun DevKitTheme(
    darkTheme: Boolean,
    accent: AccentChoice,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val scheme = if (dynamicColor) withDynamicColor(darkTheme, accent) else {
        if (darkTheme) darkColorSchemeFor(accent) else lightColorSchemeFor(accent)
    }
    val semantics = if (darkTheme) darkSemanticColors() else lightSemanticColors()
    CompositionLocalProvider(LocalSemanticColors provides semantics) {
        MaterialTheme(
            colorScheme = scheme,
            typography = DevKitTypography,
            shapes = DevKitShapes,
            content = content,
        )
    }
}

@Composable
private fun withDynamicColor(darkTheme: Boolean, accent: AccentChoice): androidx.compose.material3.ColorScheme {
    val context = androidx.compose.ui.platform.LocalContext.current
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        if (darkTheme) {
            androidx.compose.material3.dynamicDarkColorScheme(context)
        } else {
            androidx.compose.material3.dynamicLightColorScheme(context)
        }
    } else {
        if (darkTheme) darkColorSchemeFor(accent) else lightColorSchemeFor(accent)
    }
}
