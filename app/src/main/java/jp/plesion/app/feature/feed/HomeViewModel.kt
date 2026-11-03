package jp.plesion.app.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.plesion.app.core.location.LocationProvider
import jp.plesion.app.core.location.toLocationErrorMessage
import jp.plesion.app.core.ui.UiState
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.repository.PostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** ホームは現在地から半径 1km 以内の投稿だけを新しい順に表示する。 */
const val NEARBY_RADIUS_METERS = 1_000.0

class HomeViewModel(
    private val postRepository: PostRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<List<Post>>>(UiState.Loading)
    val uiState: StateFlow<UiState<List<Post>>> = _uiState.asStateFlow()

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

    private suspend fun loadNearbyPosts(): UiState<List<Post>> {
        val center = locationProvider.getCurrentLocation()
            .getOrElse { return UiState.Error(it.toLocationErrorMessage()) }
        return postRepository.getNearbyPosts(center, NEARBY_RADIUS_METERS).fold(
            onSuccess = { posts -> UiState.Success(posts.sortedByDescending { it.createdAt }) },
            onFailure = { UiState.Error("近くの投稿を読み込めませんでした。通信状況を確認してください") },
        )
    }
}
