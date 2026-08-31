package ao.cmc.fincrestsdvm.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Matches --color-fincrest-500 (#6F194E) from src/assets/css/main.css in the
// web app, the brand's primary color.
val FincrestPrimary = Color(0xFF6F194E)
val FincrestPrimaryContainer = Color(0xFFF6DDBF)
val WarningColor = Color(0xFFB45309)
val SuccessColor = Color(0xFF15803D)

private val LightColors = lightColorScheme(
    primary = FincrestPrimary,
    onPrimary = Color.White,
    primaryContainer = FincrestPrimaryContainer,
    secondary = FincrestPrimary
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE0A9C6),
    onPrimary = Color(0xFF3F0A2A),
    primaryContainer = FincrestPrimary,
    secondary = Color(0xFFE0A9C6)
)

@Composable
fun FincrestTheme(darkTheme: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
