package jp.plesion.app.feature.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.plesion.app.core.ui.UiStateContent
import jp.plesion.app.data.model.Post

// TODO(tanabe): タイムライン・投稿カード・投稿詳細への遷移を実装する
@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    UiStateContent(state = state, modifier = modifier) { posts ->
        LazyColumn(modifier = modifier) {
            items(posts, key = { it.id }) { post -> PostCardPlaceholder(post) }
        }
    }
}

@Composable
private fun PostCardPlaceholder(post: Post) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(post.authorName, style = MaterialTheme.typography.labelLarge)
            Text(post.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
