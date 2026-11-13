package jp.plesion.app.core.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

/** 下にこれだけ（dp）引っ張って離すと閉じる。 */
private const val DISMISS_DRAG_DP = 120

/**
 * 投稿の画像を全画面で表示する。左右のスワイプで画像を切り替え、ピンチ・ダブルタップで拡大する。
 * 拡大していないときに上下に引っ張ると閉じる。× ボタン・戻るボタンでも閉じる。
 */
@Composable
fun ImageViewer(urls: List<String>, initialIndex: Int, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
    ) {
        val pagerState = rememberPagerState(initialPage = initialIndex) { urls.size }
        val scope = rememberCoroutineScope()
        // 閉じるために引っ張っている量。離したときに戻すのでアニメーションできる値にする
        val dragY = remember { Animatable(0f) }
        val dismissPx = with(LocalDensity.current) { DISMISS_DRAG_DP.dp.toPx() }
        // 引っ張るほど背景を薄くする
        val backgroundAlpha = 1f - (abs(dragY.value) / (dismissPx * 3)).coerceIn(0f, 0.6f)

        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = backgroundAlpha))
                // 拡大中は子（ZoomableImage）が指の動きを使うので、ここには来ない
                .pointerInput(Unit) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (abs(dragY.value) > dismissPx) onDismiss()
                            else scope.launch { dragY.animateTo(0f) }
                        },
                        onDragCancel = { scope.launch { dragY.animateTo(0f) } },
                        onVerticalDrag = { change, amount ->
                            change.consume()
                            scope.launch { dragY.snapTo(dragY.value + amount) }
                        },
                    )
                },
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { translationY = dragY.value },
            ) { page ->
                ZoomableImage(url = urls[page], isCurrentPage = pagerState.currentPage == page)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp),
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterStart)) {
                    Icon(Icons.Default.Close, contentDescription = "閉じる", tint = Color.White)
                }
                if (urls.size > 1) {
                    Text(
                        "${pagerState.currentPage + 1} / ${urls.size}",
                        color = Color.White,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableImage(url: String, isCurrentPage: Boolean) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    // 画像の元の大きさ。読み込みが終わるまでは分からない
    var imageSize by remember { mutableStateOf(Size.Unspecified) }

    // 拡大した画像の端が画面の内側に入らないように、移動できる範囲を絞る。
    // 画像は画面に収まるように縮めて表示しているので、上下か左右に余白がある。その余白は移動できる範囲に含めない
    fun clamp(value: Offset, atScale: Float): Offset {
        val fit = if (imageSize.isSpecified && imageSize.width > 0 && imageSize.height > 0) {
            min(size.width / imageSize.width, size.height / imageSize.height)
        } else {
            0f
        }
        val shownWidth = if (fit > 0f) imageSize.width * fit else size.width.toFloat()
        val shownHeight = if (fit > 0f) imageSize.height * fit else size.height.toFloat()
        val maxX = ((shownWidth * atScale - size.width) / 2).coerceAtLeast(0f)
        val maxY = ((shownHeight * atScale - size.height) / 2).coerceAtLeast(0f)
        return Offset(value.x.coerceIn(-maxX, maxX), value.y.coerceIn(-maxY, maxY))
    }

    // 別の画像に切り替えたら、前の画像の拡大は元に戻す
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage) {
            scale = 1f
            offset = Offset.Zero
        }
    }

    AsyncImage(
        model = url,
        contentDescription = "投稿の画像",
        contentScale = ContentScale.Fit,
        onSuccess = { imageSize = it.painter.intrinsicSize },
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            // タップした場所を中心に拡大する
                            val center = Offset(size.width / 2f, size.height / 2f)
                            scale = DOUBLE_TAP_SCALE
                            offset = clamp((center - tap) * (DOUBLE_TAP_SCALE - 1), DOUBLE_TAP_SCALE)
                        }
                    },
                )
            }
            // 2 本指のとき・拡大中だけ指の動きを使う。それ以外はページ送りと、引っ張って閉じる操作に任せる
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1 || scale > 1f) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, MAX_SCALE)
                            offset = if (scale == 1f) Offset.Zero else clamp(offset + event.calculatePan(), scale)
                            // 動いた指だけ使う。動いていない指（タップ）はダブルタップの判定に回す
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y
            },
    )
}
