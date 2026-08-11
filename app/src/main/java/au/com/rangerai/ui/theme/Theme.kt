package au.com.rangerai.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val IceBluePrimary = Color(0xFF00D4FF)
val IceBlueLight = Color(0xFF66E5FF)
val IceBlueDark = Color(0xFF0099CC)
val IceBlueGlow = Color(0xFF00B8E6)
val IceBlueDim = Color(0xFF005577)
val AccentOrange = Color(0xFFFF8C00)
val AccentGreen = Color(0xFF00E676)
val AccentRed = Color(0xFFFF1744)
val AccentYellow = Color(0xFFFFD600)
val DarkBackground = Color(0xFF060A0D)
val SurfaceBlack = Color(0xFF0A0A0A)
val SurfaceCard = Color(0xFF111418)
val SurfaceElevated = Color(0xFF1A1E24)
val SurfaceGauge = Color(0xFF0D1012)

val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0BEC5)
val TextMuted = Color(0xFF607D8B)

private val DarkColorScheme = darkColorScheme(
    primary = IceBluePrimary,
    secondary = IceBlueLight,
    tertiary = AccentOrange,
    background = SurfaceBlack,
    surface = SurfaceCard,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
)

val AppTypography = Typography()

@Composable
fun FordGuardianTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SurfaceBlack.toArgb()
            window.navigationBarColor = SurfaceBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
