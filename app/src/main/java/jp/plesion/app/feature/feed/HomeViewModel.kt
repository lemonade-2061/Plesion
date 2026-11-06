package jp.plesion.app.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.plesion.app.core.location.GeoUtils
import jp.plesion.app.core.location.LocationProvider
import jp.plesion.app.core.location.toLocationErrorMessage
import jp.plesion.app.core.ui.UiState
import jp.plesion.app.data.model.GeoPoint
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.repository.PostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** ホームは現在地から半径 1km 以内の投稿だけを新しい順に表示する。 */
const val NEARBY_RADIUS_METERS = 1_000.0

/** ホーム画面の表示用データ。Post から toFeedPost() で変換して作る。 */
data class FeedPost(
    val id: String,
    val nickname: String,
    val timeAgo: String,
    val distance: String,
    val body: String,
    val commentCount: Int,
)

class HomeViewModel(
    private val postRepository: PostRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<FeedPost>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<FeedPost>>> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var loadJob: Job? = null
    private var rerunRequested = false

    /**
     * 現在地を取り直して周辺の投稿を読み込む。取得中に呼ばれても並行しては実行しない。
     * ただし取得中の要求は捨てず、終わった後に 1 回だけ取り直す（取得中の投稿完了・移動を取りこぼさないため）。
     * 一覧を表示済みなら、更新中も前の一覧を残す（詳細から戻ったときなどに画面が消えないように）。
     */
    fun refresh() {
        if (loadJob?.isActive == true) {
            rerunRequested = true
            return
        }
        loadJob = viewModelScope.launch {
            if (_uiState.value !is UiState.Success) _uiState.value = UiState.Loading
            _isRefreshing.value = true
            do {
                rerunRequested = false
                _uiState.value = loadNearbyPosts()
            } while (rerunRequested)
            _isRefreshing.value = false
        }
    }

    private suspend fun loadNearbyPosts(): UiState<List<FeedPost>> {
        val center = locationProvider.getCurrentLocation()
            .getOrElse { return UiState.Error(it.toLocationErrorMessage()) }
        return postRepository.getNearbyPosts(center, NEARBY_RADIUS_METERS).fold(
            onSuccess = { posts ->
                UiState.Success(
                    posts.sortedByDescending { it.createdAt }.map { it.toFeedPost(center) }
                )
            },
            onFailure = { UiState.Error("近くの投稿を読み込めませんでした。通信状況を確認してください") },
        )
    }
}

private fun Post.toFeedPost(center: GeoPoint): FeedPost = FeedPost(
    id = id,
    nickname = authorName,
    timeAgo = formatTimeAgo(createdAt),
    distance = location?.let { formatDistance(GeoUtils.distanceMeters(center, it)) }.orEmpty(),
    body = text,
    commentCount = commentCount,
)

/** エポックミリ秒から「たった今 / 〇分前 / 〇時間前 / 〇日前」を作る。 */
private fun formatTimeAgo(createdAtMillis: Long): String {
    val diffMillis = (System.currentTimeMillis() - createdAtMillis).coerceAtLeast(0L)
    val minutes = diffMillis / 60_000L
    return when {
        minutes < 1L -> "たった今"
        minutes < 60L -> "${minutes}分前"
        minutes < 60L * 24L -> "${minutes / 60L}時間前"
        else -> "${minutes / (60L * 24L)}日前"
    }
}

/** 10m 単位に丸めて「〇〇m」にする。 */
private fun formatDistance(meters: Double): String {
    val rounded = (meters / 10.0).roundToInt() * 10
    return "${rounded}m"
}