package com.matthew.sportiliapp.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

data class SportiliStatusColors(
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val infoContainer: Color,
    val onInfoContainer: Color
)

private val LightStatusColors = SportiliStatusColors(
    successContainer = SuccessContainerLight,
    onSuccessContainer = OnSuccessContainerLight,
    warningContainer = WarningContainerLight,
    onWarningContainer = OnWarningContainerLight,
    infoContainer = InfoContainerLight,
    onInfoContainer = OnInfoContainerLight
)

private val DarkStatusColors = SportiliStatusColors(
    successContainer = SuccessContainerDark,
    onSuccessContainer = OnSuccessContainerDark,
    warningContainer = WarningContainerDark,
    onWarningContainer = OnWarningContainerDark,
    infoContainer = InfoContainerDark,
    onInfoContainer = OnInfoContainerDark
)

private val LocalSportiliStatusColors = staticCompositionLocalOf { LightStatusColors }

val MaterialTheme.sportiliStatusColors: SportiliStatusColors
    @Composable
    @ReadOnlyComposable
    get() = LocalSportiliStatusColors.current

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = SurfaceMutedDark,
    onPrimaryContainer = OnSurfaceMutedDark,
    secondary = PrimaryDark,
    onSecondary = OnPrimaryDark,
    tertiary = BrandAccent,
    onTertiary = OnBrandAccent,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = OnSurfaceMutedDark,
    outline = OutlineDark,
    outlineVariant = SurfaceMutedDark,
    error = OnCriticalContainerDark,
    onError = CriticalContainerDark,
    errorContainer = CriticalContainerDark,
    onErrorContainer = OnCriticalContainerDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = SurfaceMutedLight,
    onPrimaryContainer = OnSurfaceMutedLight,
    secondary = PrimaryLight,
    onSecondary = OnPrimaryLight,
    tertiary = BrandAccent,
    onTertiary = OnBrandAccent,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceMutedLight,
    onSurfaceVariant = OnSurfaceMutedLight,
    outline = OutlineLight,
    outlineVariant = SurfaceMutedLight,
    error = OnCriticalContainerLight,
    onError = CriticalContainerLight,
    errorContainer = CriticalContainerLight,
    onErrorContainer = OnCriticalContainerLight
)

@Composable
fun SportiliAppTheme(
    isDarkTheme: Boolean = isSystemInDarkTheme(),
    isDynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val dynamicColor = isDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val colorScheme = when {
        dynamicColor && isDarkTheme -> dynamicDarkColorScheme(LocalContext.current)
        dynamicColor -> dynamicLightColorScheme(LocalContext.current)
        isDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val statusColors = if (isDarkTheme) DarkStatusColors else LightStatusColors

    CompositionLocalProvider(LocalSportiliStatusColors provides statusColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = CustomTypography,
            content = content
        )
    }
}
