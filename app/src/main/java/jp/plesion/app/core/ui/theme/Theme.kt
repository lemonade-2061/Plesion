package jp.plesion.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * 紫を基調にした 1 種類の配色。各画面が紫の背景で作られているので、端末のダークモードでは切り替えない。
 * 共通部品（ローディング・エラー・位置情報の案内）やダイアログ・ナビゲーションバーもこの配色で表示される。
 */
private val PlesionColors = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    primaryContainer = AccentDark,
    onPrimaryContainer = Color.White,
    secondary = CardPurple,
    onSecondary = Color.White,
    secondaryContainer = AvatarPurple,
    onSecondaryContainer = Color.White,
    tertiary = CardPurple,
    onTertiary = Color.White,
    background = BgPurple,
    onBackground = Color.White,
    surface = BgPurple,
    onSurface = Color.White,
    surfaceVariant = AvatarPurple,
    onSurfaceVariant = Color(0xFFE6D2EC),
    surfaceContainerLowest = SurfaceContainerLowest,
    surfaceContainerLow = SurfaceContainerLow,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    error = ErrorText,
    onError = Color(0xFF690005),
    outline = CardPurple,
    outlineVariant = AvatarPurple,
)

@Composable
fun PlesionTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = PlesionColors, content = content)
}
