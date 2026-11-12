package jp.plesion.app.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * 投稿の画像（最大 4 枚）を横に並べる。1 枚のときは横幅いっぱいに出す。
 * [urls] は Storage のダウンロード URL（Fake のときは端末内の content:// の Uri）。
 */
@Composable
fun PostImages(urls: List<String>, modifier: Modifier = Modifier) {
    if (urls.isEmpty()) return
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        urls.forEach { url ->
            AsyncImage(
                model = url,
                contentDescription = "投稿の画像",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(if (urls.size == 1) 4f / 3f else 1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.2f)),
            )
        }
    }
}
