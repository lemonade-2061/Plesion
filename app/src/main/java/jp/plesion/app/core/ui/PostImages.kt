package jp.plesion.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * 投稿の画像（最大 4 枚）を横に並べる。1 枚のときは横幅いっぱいに出す。
 * [urls] は Storage のダウンロード URL（Fake のときは端末内の content:// の Uri）。
 * 画像を押すと全画面で表示する（[ImageViewer]）。投稿カード自体のクリックより画像のクリックが優先される。
 */
@Composable
fun PostImages(urls: List<String>, modifier: Modifier = Modifier) {
    if (urls.isEmpty()) return
    // 全画面で表示中の画像の番号。null なら閉じている
    var viewerIndex by remember { mutableStateOf<Int?>(null) }
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        urls.forEachIndexed { index, url ->
            AsyncImage(
                model = url,
                contentDescription = "投稿の画像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(if (urls.size == 1) 4f / 3f else 1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
                    .clickable { viewerIndex = index },
            )
        }
    }
    viewerIndex?.let { index ->
        ImageViewer(urls = urls, initialIndex = index, onDismiss = { viewerIndex = null })
    }
}
