package jp.plesion.app.feature.post

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// TODO(tajima): 投稿作成画面の本実装（画像添付・文字数制限など）
@Composable
fun CreatePostScreen(
    viewModel: CreatePostViewModel,
    onPosted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isPosted) {
        if (state.isPosted) onPosted()
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = state.text,
            onValueChange = viewModel::onTextChange,
            label = { Text("いまどうしてる？") },
            modifier = Modifier.fillMaxWidth(),
        )
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(onClick = viewModel::submit, enabled = !state.isSubmitting) {
            Text("投稿する（仮データに追加）")
        }
    }
}
