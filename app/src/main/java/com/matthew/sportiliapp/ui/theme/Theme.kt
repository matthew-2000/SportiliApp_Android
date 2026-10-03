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
    inversePrimary = PrimaryLight,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SurfaceMutedDark,
    onSecondaryContainer = OnSurfaceMutedDark,
    tertiary = OnWarningContainerDark,
    onTertiary = WarningContainerDark,
    tertiaryContainer = WarningContainerDark,
    onTertiaryContainer = OnWarningContainerDark,
    background = BackgroundDark,
    onBackground = OnSurfaceDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceMutedDark,
    onSurfaceVariant = OnSurfaceMutedDark,
    surfaceTint = PrimaryDark,
    inverseSurface = SurfaceMutedLight,
    inverseOnSurface = OnSurfaceLight,
    surfaceBright = SurfaceBrightDark,
    surfaceDim = BackgroundDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceMutedDark,
    outline = OutlineDark,
    outlineVariant = SurfaceMutedDark,
    error = OnCriticalContainerDark,
    onError = CriticalContainerDark,
    errorContainer = CriticalContainerDark,
    onErrorContainer = OnCriticalContainerDark,
    scrim = Color.Black
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = SurfaceMutedLight,
    onPrimaryContainer = OnSurfaceMutedLight,
    inversePrimary = PrimaryDark,
    secondary = SecondaryLight,
    onSecondary = OnPrimaryLight,
    secondaryContainer = SurfaceMutedLight,
    onSecondaryContainer = OnSurfaceMutedLight,
    tertiary = OnWarningContainerLight,
    onTertiary = OnPrimaryLight,
    tertiaryContainer = WarningContainerLight,
    onTertiaryContainer = OnWarningContainerLight,
    background = BackgroundLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceMutedLight,
    onSurfaceVariant = OnSurfaceMutedLight,
    surfaceTint = PrimaryLight,
    inverseSurface = SurfaceMutedDark,
    inverseOnSurface = OnSurfaceDark,
    surfaceBright = SurfaceLight,
    surfaceDim = SurfaceDimLight,
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceMutedLight,
    outline = OutlineLight,
    outlineVariant = SurfaceMutedLight,
    error = OnCriticalContainerLight,
    onError = CriticalContainerLight,
    errorContainer = CriticalContainerLight,
    onErrorContainer = OnCriticalContainerLight,
    scrim = Color.Black
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
            shapes = SportiliShapes,
            content = content
        )
    }
}
