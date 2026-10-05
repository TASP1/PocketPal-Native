package com.tasp1.pocketpal.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.tasp1.pocketpal.data.AppState

data class PpExtraColors(
    val userBubble: Color,
    val assistantBubble: Color,
    val inputBar: Color,
    val placeholder: Color,
    val accentPeach: Color,
)

val LocalPpExtra = staticCompositionLocalOf {
    PpExtraColors(
        userBubble = PpLight.UserBubble,
        assistantBubble = PpLight.AssistantBubble,
        inputBar = PpLight.InputBar,
        placeholder = PpLight.Placeholder,
        accentPeach = PpLight.AccentPeach,
    )
}

private val LightScheme = lightColorScheme(
    primary = PpLight.Primary,
    onPrimary = PpLight.OnPrimary,
    primaryContainer = PpLight.PrimaryContainer,
    onPrimaryContainer = PpLight.OnPrimaryContainer,
    secondary = PpLight.Secondary,
    onSecondary = PpLight.OnSecondary,
    secondaryContainer = PpLight.SecondaryContainer,
    onSecondaryContainer = PpLight.OnSecondaryContainer,
    tertiary = PpLight.Tertiary,
    error = PpLight.Error,
    background = PpLight.Background,
    onBackground = PpLight.OnBackground,
    surface = PpLight.Surface,
    onSurface = PpLight.OnSurface,
    surfaceVariant = PpLight.SurfaceVariant,
    onSurfaceVariant = PpLight.OnSurfaceVariant,
    outline = PpLight.Outline,
    outlineVariant = PpLight.OutlineVariant,
)

private val DarkScheme = darkColorScheme(
    primary = PpDark.Primary,
    onPrimary = PpDark.OnPrimary,
    primaryContainer = PpDark.PrimaryContainer,
    onPrimaryContainer = PpDark.OnPrimaryContainer,
    secondary = PpDark.Secondary,
    onSecondary = PpDark.OnSecondary,
    tertiary = PpDark.Tertiary,
    error = PpDark.Error,
    background = PpDark.Background,
    onBackground = PpDark.OnBackground,
    surface = PpDark.Surface,
    onSurface = PpDark.OnSurface,
    surfaceVariant = PpDark.SurfaceVariant,
    onSurfaceVariant = PpDark.OnSurfaceVariant,
    outline = PpDark.Outline,
)

@Composable
fun PocketPalTheme(content: @Composable () -> Unit) {
    val systemDark = isSystemInDarkTheme()
    val darkTheme = AppState.darkTheme ?: systemDark
    val extra = if (darkTheme) {
        PpExtraColors(
            userBubble = PpDark.UserBubble,
            assistantBubble = PpDark.AssistantBubble,
            inputBar = PpDark.InputBar,
            placeholder = PpDark.Placeholder,
            accentPeach = PpDark.AccentPeach,
        )
    } else {
        PpExtraColors(
            userBubble = PpLight.UserBubble,
            assistantBubble = PpLight.AssistantBubble,
            inputBar = PpLight.InputBar,
            placeholder = PpLight.Placeholder,
            accentPeach = PpLight.AccentPeach,
        )
    }
    CompositionLocalProvider(LocalPpExtra provides extra) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = PpTypography,
            content = content,
        )
    }
}
