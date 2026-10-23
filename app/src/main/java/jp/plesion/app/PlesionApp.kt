package jp.plesion.app

import androidx.compose.runtime.Composable
import jp.plesion.app.core.ui.theme.PlesionTheme
import jp.plesion.app.di.AppContainer
import jp.plesion.app.navigation.PlesionNavHost

/** アプリのルート Composable。テーマとナビゲーションを組み立てるだけにする。 */
@Composable
fun PlesionApp(container: AppContainer) {
    PlesionTheme {
        PlesionNavHost(container = container)
    }
}
