package br.com.yson.controle.de.obras.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val HighContrastDarkColorScheme =
  darkColorScheme(
    primary = PrimaryMinimal,
    onPrimary = OnPrimaryMinimal,
    primaryContainer = PrimaryContainerMinimal,
    onPrimaryContainer = OnPrimaryContainerMinimal,
    secondary = SecondaryMinimal,
    onSecondary = OnSecondaryMinimal,
    background = BackgroundMinimal,
    onBackground = OnBackgroundMinimal,
    surface = SurfaceMinimal,
    onSurface = OnSurfaceMinimal,
    surfaceVariant = AccountBg,
    onSurfaceVariant = MutedGrey,
    outline = OutlinedBorder
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is disabled by default to force our beautiful Clean Minimal theme
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = HighContrastDarkColorScheme // Force High-Contrast Dark Color Scheme for outstanding visual clarity and style consistency


  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
