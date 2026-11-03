package jp.plesion.app.feature.feed.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.plesion.app.core.ui.UiStateContent
import jp.plesion.app.data.model.Comment
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

internal fun formatDateTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

// TODO(tanabe): 画像の表示（画像読み込みライブラリ未導入）・コメント送信 UI（addComment は Repository に実装済み）
@Composable
fun PostDetailScreen(
    viewModel: PostDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(modifier = modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) { Text("← 戻る") }
        UiStateContent(state = state, onRetry = viewModel::load) { detail ->
            val post = detail.post
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(post.authorName, style = MaterialTheme.typography.labelLarge)
                            Text(post.text, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                formatDateTime(post.createdAt),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                item {
                    Text(
                        "コメント ${post.commentCount}件",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                if (detail.comments.isEmpty()) {
                    item {
                        Text(
                            "まだコメントはありません",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                items(detail.comments, key = { it.id }) { comment -> CommentItem(comment) }
            }
        }
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(comment.authorName, style = MaterialTheme.typography.labelLarge)
        Text(comment.text, style = MaterialTheme.typography.bodyMedium)
        Text(
            formatDateTime(comment.createdAt),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
    }
}
