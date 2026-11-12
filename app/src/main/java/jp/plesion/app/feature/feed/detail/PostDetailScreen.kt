package jp.plesion.app.feature.feed.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.plesion.app.core.ui.PostImages
import jp.plesion.app.core.ui.UiState
import jp.plesion.app.core.ui.UiStateContent
import jp.plesion.app.data.model.Comment
import jp.plesion.app.data.model.Post
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val BgPurple = Color(0xFF42145F)
private val CardPurple = Color(0xFFA060B0)
private val AvatarPurple = Color(0xFF7B3A8C)
private val CommentGray = Color(0xFFD9D9D9)
private val ErrorText = Color(0xFFFFB4AB)

private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")

internal fun formatDateTime(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
    dateTimeFormatter.format(Instant.ofEpochMilli(epochMillis).atZone(zone))

@Composable
fun PostDetailScreen(
    viewModel: PostDetailViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val commentInput by viewModel.commentInput.collectAsStateWithLifecycle()
    val postAction by viewModel.postAction.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }

    // 削除できたら前の画面に戻る（ホーム・プロフィールは戻ったときに読み直す）
    LaunchedEffect(postAction.isDeleted) {
        if (postAction.isDeleted) onBack()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = BgPurple,
        contentColor = Color.White,
    ) {
        // キーボードが出たときに入力欄が隠れないよう、画面全体をキーボードの上に収める
        Column(Modifier.fillMaxSize().imePadding()) {
            DetailTopBar(
                onBack = onBack,
                showActions = postAction.isMine && !postAction.isWorking,
                onEdit = viewModel::startEdit,
                onDelete = { confirmDelete = true },
            )
            // 編集ダイアログの外で起きた失敗（削除の失敗）はここに出す
            if (postAction.editingText == null) {
                postAction.errorMessage?.let {
                    Text(it, color = ErrorText, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                }
            }

            // 上バーの下の残り全部を、読み込み中・エラー・一覧の表示に使う
            Box(Modifier.weight(1f).fillMaxWidth()) {
                UiStateContent(
                    state = state,
                    onRetry = viewModel::load,
                    modifier = Modifier.fillMaxSize(),
                ) { detail ->
                    val post = detail.post
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                    ) {
                        // 投稿全体（本文は高さを固定せず、長さに合わせて全文を表示する）
                        item {
                            AuthorRow(
                                name = post.authorName,
                                time = formatDateTime(post.createdAt) + if (post.editedAt != null) "（編集済み）" else "",
                                avatarSize = 48,
                            )
                            Column(
                                Modifier
                                    .padding(horizontal = 16.dp)
                                    .fillMaxWidth()
                                    .background(CardPurple, RoundedCornerShape(24.dp))
                                    .padding(16.dp),
                            ) {
                                Text(
                                    text = post.text,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    lineHeight = 20.sp,
                                )
                                if (post.imageUrls.isNotEmpty()) {
                                    Spacer(Modifier.height(10.dp))
                                    PostImages(post.imageUrls)
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    // コメント（件数つき）
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Email,
                                            contentDescription = "コメント",
                                            tint = Color.White,
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            detail.comments.size.toString(),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                        )
                                    }
                                    // リポスト・いいねは見た目だけ
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "リポスト",
                                        tint = Color.White,
                                    )
                                    Icon(
                                        Icons.Default.FavoriteBorder,
                                        contentDescription = "いいね",
                                        tint = Color.White,
                                    )
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                        }

                        // 返信
                        item {
                            Text(
                                text = "コメント（${detail.comments.size}）",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            )
                        }
                        if (detail.comments.isEmpty()) {
                            item {
                                Text(
                                    text = "まだコメントはありません",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                )
                            }
                        }
                        items(detail.comments, key = { it.id }) { comment ->
                            CommentItem(comment)
                        }
                    }
                }
            }

            // 投稿を読み込めたときだけ、コメントを書ける
            if (state is UiState.Success) {
                CommentInputBar(
                    input = commentInput,
                    onTextChange = viewModel::onCommentTextChange,
                    onSend = viewModel::sendComment,
                )
            }
        }
    }

    postAction.editingText?.let { text ->
        EditPostDialog(
            text = text,
            isSaving = postAction.isWorking,
            errorMessage = postAction.errorMessage,
            onTextChange = viewModel::onEditTextChange,
            onSave = viewModel::saveEdit,
            onCancel = viewModel::cancelEdit,
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("投稿を削除しますか？") },
            text = { Text("コメントもすべて削除され、元に戻せません。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.deletePost()
                }) { Text("削除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("キャンセル") }
            },
        )
    }
}

@Composable
private fun EditPostDialog(
    text: String,
    isSaving: Boolean,
    errorMessage: String?,
    onTextChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isSaving) onCancel() },
        title = { Text("投稿を編集") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    enabled = !isSaving,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                )
                Text(
                    "${text.length}/${Post.MAX_TEXT_LENGTH}文字",
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                )
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 13.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = !isSaving && text.isNotBlank()) {
                Text(if (isSaving) "保存中…" else "保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, enabled = !isSaving) { Text("キャンセル") }
        },
    )
}

@Composable
private fun CommentInputBar(
    input: CommentInputState,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(BgPurple)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        input.errorMessage?.let {
            Text(it, color = ErrorText, fontSize = 12.sp, modifier = Modifier.padding(start = 8.dp, bottom = 4.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = input.text,
                onValueChange = onTextChange,
                placeholder = { Text("コメントを書く", color = Color.White.copy(alpha = 0.6f)) },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = CardPurple,
                    unfocusedContainerColor = CardPurple,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = Color.White,
                ),
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            if (input.isSending) {
                CircularProgressIndicator(Modifier.size(24.dp).padding(2.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                IconButton(onClick = onSend, enabled = input.text.isNotBlank()) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "コメントを送信",
                        tint = if (input.text.isNotBlank()) Color.White else Color.White.copy(alpha = 0.4f),
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailTopBar(
    onBack: () -> Unit,
    showActions: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    // 状態バーの余白は、PlesionNavHost の Scaffold がすでに取っているので、ここでは付けない
    Box(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.align(Alignment.CenterStart).clickable { onBack() },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "戻る",
                tint = Color.Black,
            )
            Spacer(Modifier.width(4.dp))
            Text("戻る", color = Color.Black, fontSize = 16.sp)
        }
        Text(
            "投稿",
            color = Color.Black,
            fontSize = 20.sp,
            modifier = Modifier.align(Alignment.Center),
        )
        // 自分の投稿のときだけ、編集・削除を出す
        if (showActions) {
            Row(Modifier.align(Alignment.CenterEnd)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "編集", tint = Color.Black)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "削除", tint = Color.Black)
                }
            }
        }
    }
}

@Composable
private fun DetailAvatar(size: Int) {
    Box(
        Modifier.size(size.dp).background(AvatarPurple, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Default.Person,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size((size * 0.6f).dp),
        )
    }
}

@Composable
private fun AuthorRow(name: String, time: String, avatarSize: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DetailAvatar(avatarSize)
        Spacer(Modifier.width(12.dp))
        Text(
            name,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(time, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun CommentItem(comment: Comment) {
    Column(Modifier.padding(bottom = 12.dp)) {
        AuthorRow(
            name = comment.authorName,
            time = formatDateTime(comment.createdAt),
            avatarSize = 40,
        )
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .background(CommentGray, RoundedCornerShape(16.dp))
                .padding(12.dp),
        ) {
            Text(comment.text, color = Color.Black, fontWeight = FontWeight.Bold)
        }
    }
}