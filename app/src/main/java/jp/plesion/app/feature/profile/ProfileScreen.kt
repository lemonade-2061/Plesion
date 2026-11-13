package jp.plesion.app.feature.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.plesion.app.core.ui.UiStateContent
import jp.plesion.app.core.ui.theme.Accent
import jp.plesion.app.core.ui.theme.AccentDark
import jp.plesion.app.core.ui.theme.AvatarPurple
import jp.plesion.app.core.ui.theme.BgPurple
import jp.plesion.app.core.ui.theme.CardPurple

/** プロフィール編集の機能ができるまでは false にして、押せるのに何も起きないボタンを隠す。 */
private const val SHOW_EDIT_BUTTON = false

/** 「自分の投稿」に表示する1件分。 */
data class ProfilePostUi(
    val id: String,
    val body: String,
    val timeAgo: String,
    val commentCount: Int,
    val isAnonymous: Boolean = false,
)

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onPostClick: (String) -> Unit = {},
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val posts by viewModel.posts.collectAsStateWithLifecycle()

    // 詳細で編集・削除して戻ってきたときに、一覧を新しくする
    LifecycleResumeEffect(Unit) {
        viewModel.loadPosts()
        onPauseOrDispose {}
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = BgPurple,
        contentColor = Color.White,
    ) {
        Column(Modifier.fillMaxSize()) {
            // 設定ボタンは、読み込み中・エラーのときも押せるように、状態の外に置く
            ProfileTopBar(onOpenSettings = onOpenSettings)

            Box(Modifier.weight(1f).fillMaxWidth()) {
                UiStateContent(
                    state = state,
                    modifier = Modifier.fillMaxSize(),
                    onRetry = viewModel::load,
                ) { user ->
                    ProfileContent(
                        displayName = user.displayName,
                        bio = user.bio,
                        // null のあいだ（読み込み中・失敗）は「自分の投稿」の欄ごと表示しない
                        posts = posts,
                        onPostClick = onPostClick,
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileContent(
    displayName: String,
    bio: String,
    posts: List<ProfilePostUi>?,
    modifier: Modifier = Modifier,
    onPostClick: (String) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item { ProfileHeader(displayName = displayName, bio = bio) }

        if (posts != null) {
            item {
                HorizontalDivider(color = Accent.copy(alpha = 0.6f))
                Text(
                    "自分の投稿",
                    color = Color.White,
                    fontSize = 15.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            if (posts.isEmpty()) {
                item {
                    Text(
                        "まだ投稿はありません",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            items(posts, key = { it.id }) { post -> MyPostCard(post, onClick = { onPostClick(post.id) }) }
        }
    }
}

@Composable
private fun ProfileTopBar(onOpenSettings: () -> Unit) {
    // 状態バーの余白は、PlesionNavHost の Scaffold がすでに取っているので、ここでは付けない
    Column {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            Text(
                "プロフィール",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center),
            )
            IconButton(
                onClick = onOpenSettings,
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(Icons.Default.Settings, contentDescription = "設定", tint = Color.White)
            }
        }
        HorizontalDivider(color = Accent.copy(alpha = 0.6f))
    }
}

@Composable
private fun ProfileHeader(displayName: String, bio: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(AvatarPurple)
                .border(2.dp, Accent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(72.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            displayName,
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
        )
        if (bio.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                bio,
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp),
            )
        }
        if (SHOW_EDIT_BUTTON) {
            Spacer(Modifier.height(16.dp))
            // TODO: 編集画面ができたら onClick をつなぐ
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Accent, AccentDark)))
                    .padding(horizontal = 28.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("プロフィールを編集", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun MyPostCard(post: ProfilePostUi, onClick: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardPurple)
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(
            post.body,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (post.isAnonymous) "${post.timeAgo}・匿名で投稿" else post.timeAgo,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
        )
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Email,
                contentDescription = "コメント",
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                post.commentCount.toString(),
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// プレビュー用の表示確認データ。アプリ本体の画面には出ない
@Preview(showBackground = true, widthDp = 360, heightDp = 700)
@Composable
private fun ProfileContentPreview() {
    Column(Modifier.background(BgPurple)) {
        ProfileTopBar(onOpenSettings = {})
        ProfileContent(
            displayName = "ユーザー名",
            bio = "自己紹介が入ります",
            posts = listOf(
                ProfilePostUi("1", "投稿の本文がここに入ります。長いときは1行で省略されます", "30分前", 3),
                ProfilePostUi("2", "二つ目の投稿", "昨日", 4),
            ),
        )
    }
}