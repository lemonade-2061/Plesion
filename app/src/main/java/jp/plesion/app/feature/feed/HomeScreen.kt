package jp.plesion.app.feature.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.plesion.app.core.ui.LocationPermissionGate
import jp.plesion.app.core.ui.UiStateContent
import jp.plesion.app.data.model.Post

// TODO(tanabe): 投稿カードの本デザイン（投稿日時・画像など）
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPostClick: (postId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LocationPermissionGate(modifier = modifier) {
        val state by viewModel.uiState.collectAsStateWithLifecycle()
        val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

        // 初回表示・投稿作成や詳細からの復帰・設定アプリからの復帰のたびに現在地から取り直す
        LifecycleResumeEffect(Unit) {
            viewModel.refresh()
            onPauseOrDispose {}
        }

        UiStateContent(state = state, modifier = modifier, onRetry = viewModel::refresh) { posts ->
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = modifier.fillMaxSize(),
            ) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (posts.isEmpty()) {
                        item {
                            Text(
                                text = "1km以内に投稿はまだありません",
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                            )
                        }
                    }
                    items(posts, key = { it.id }) { post ->
                        PostCardPlaceholder(post, onClick = { onPostClick(post.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun PostCardPlaceholder(post: Post, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(post.authorName, style = MaterialTheme.typography.labelLarge)
            Text(post.text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
