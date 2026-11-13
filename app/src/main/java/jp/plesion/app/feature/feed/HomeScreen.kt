package jp.plesion.app.feature.feed

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import jp.plesion.app.core.ui.LocationPermissionGate
import jp.plesion.app.core.ui.PostImages
import jp.plesion.app.core.ui.UiStateContent
import jp.plesion.app.core.ui.theme.AvatarPurple
import jp.plesion.app.core.ui.theme.BgPurple
import jp.plesion.app.core.ui.theme.CardPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onPostClick: (postId: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = BgPurple,
        contentColor = Color.White,
    ) {
        Column(Modifier.fillMaxSize()) {
            val areaName by viewModel.areaName.collectAsStateWithLifecycle()
            HomeTopBar(areaName)

            // バーの下の残り全部を、権限チェック+一覧に使う
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LocationPermissionGate(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.uiState.collectAsStateWithLifecycle()
                    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

                    // 初回表示・投稿作成や詳細からの復帰・設定アプリからの復帰のたびに現在地から取り直す
                    LifecycleResumeEffect(Unit) {
                        viewModel.refresh()
                        onPauseOrDispose {}
                    }

                    UiStateContent(
                        state = state,
                        modifier = Modifier.fillMaxSize(),
                        onRetry = viewModel::refresh,
                    ) { posts ->
                        PullToRefreshBox(
                            isRefreshing = isRefreshing,
                            onRefresh = viewModel::refresh,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                if (posts.isEmpty()) {
                                    item {
                                        Text(
                                            text = "1km以内に投稿はまだありません",
                                            color = Color.White,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                                        )
                                    }
                                }
                                items(posts, key = { it.id }) { post ->
                                    PostItem(post, onClick = { onPostClick(post.id) })
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.5f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeTopBar(areaName: String?) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFE8502F))
        Spacer(Modifier.width(8.dp))
        Text(
            if (areaName != null) "${areaName}エリア" else "現在地周辺",
            color = Color.Black,
            fontSize = 18.sp,
            modifier = Modifier.weight(1f),
        )
        Icon(Icons.Default.Notifications, contentDescription = null, tint = Color.Black)
    }
}

@Composable
private fun Avatar(size: Int = 48) {
    Box(
        Modifier
            .size(size.dp)
            .background(AvatarPurple, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text("アイコン", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun PostHeader(post: FeedPost) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar()
        Spacer(Modifier.width(12.dp))
        Text(
            post.nickname,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(post.timeAgo, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        if (post.distance.isNotEmpty()) {
            Spacer(Modifier.width(24.dp))
            Text(post.distance, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
    }
}

@Composable
private fun PostBubble(post: FeedPost) {
    Column(
        Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .background(CardPurple, RoundedCornerShape(24.dp))
            .padding(16.dp),
    ) {
        Text(
            post.body,
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
                Icon(Icons.Default.Email, contentDescription = "コメント", tint = Color.White)
                Spacer(Modifier.width(6.dp))
                Text(
                    post.commentCount.toString(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
            // リポスト・いいねは見た目だけ
            Icon(Icons.Default.Refresh, contentDescription = "リポスト", tint = Color.White)
            Icon(Icons.Default.FavoriteBorder, contentDescription = "いいね", tint = Color.White)
        }
    }
}

@Composable
private fun PostItem(post: FeedPost, onClick: () -> Unit) {
    Column(
        Modifier
            .clickable { onClick() }
            .padding(bottom = 12.dp),
    ) {
        PostHeader(post)
        PostBubble(post)
    }
}