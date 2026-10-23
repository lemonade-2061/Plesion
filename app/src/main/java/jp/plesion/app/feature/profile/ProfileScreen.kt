package jp.plesion.app.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.plesion.app.core.ui.UiStateContent

// TODO(tajima): プロフィール表示・編集を実装する
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateContent(state = state, modifier = modifier, onRetry = viewModel::load) { user ->
        Column(modifier = modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(user.displayName, style = MaterialTheme.typography.headlineSmall)
            Text(user.bio, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onOpenSettings) { Text("設定") }
        }
    }
}
