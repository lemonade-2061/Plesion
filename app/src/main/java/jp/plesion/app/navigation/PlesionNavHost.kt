package jp.plesion.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import jp.plesion.app.di.AppContainer
import jp.plesion.app.feature.auth.LoginScreen
import jp.plesion.app.feature.auth.LoginViewModel
import jp.plesion.app.feature.feed.HomeScreen
import jp.plesion.app.feature.feed.HomeViewModel
import jp.plesion.app.feature.post.CreatePostScreen
import jp.plesion.app.feature.post.CreatePostViewModel
import jp.plesion.app.feature.profile.ProfileScreen
import jp.plesion.app.feature.profile.ProfileViewModel
import jp.plesion.app.feature.settings.SettingsScreen

private data class BottomTab(val route: String, val label: String)

private val bottomTabs = listOf(
    BottomTab(Routes.HOME, "ホーム"),
    BottomTab(Routes.CREATE_POST, "投稿"),
    BottomTab(Routes.PROFILE, "プロフィール"),
)

@Composable
fun PlesionNavHost(
    container: AppContainer,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomTabs.map { it.route }) {
                NavigationBar {
                    bottomTabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = {}, // TODO(tanabe): アイコンを決める
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOGIN,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    viewModel = viewModel(factory = viewModelFactory {
                        initializer { LoginViewModel(container.authRepository) }
                    }),
                    onSignedIn = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    viewModel = viewModel(factory = viewModelFactory {
                        initializer { HomeViewModel(container.postRepository) }
                    }),
                )
            }
            composable(Routes.CREATE_POST) {
                CreatePostScreen(
                    viewModel = viewModel(factory = viewModelFactory {
                        initializer { CreatePostViewModel(container.postRepository) }
                    }),
                    onPosted = { navController.navigateToTab(Routes.HOME) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    viewModel = viewModel(factory = viewModelFactory {
                        initializer { ProfileViewModel(container.userRepository) }
                    }),
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}

private fun NavHostController.navigateToTab(route: String) {
    navigate(route) {
        // ログイン画面はスタックから消えるので、ホームを基準にタブを切り替える
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
