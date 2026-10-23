package jp.plesion.app.feature.feed

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.plesion.app.core.ui.UiState
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.repository.PostRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(postRepository: PostRepository) : ViewModel() {
    val uiState: StateFlow<UiState<List<Post>>> = postRepository.observeTimeline()
        .map<List<Post>, UiState<List<Post>>> { UiState.Success(it) }
        .catch { emit(UiState.Error(it.message ?: "タイムラインを読み込めませんでした")) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UiState.Loading)
}
