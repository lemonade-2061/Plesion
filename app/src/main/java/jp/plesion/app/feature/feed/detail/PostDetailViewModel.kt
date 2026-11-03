package jp.plesion.app.feature.feed.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.plesion.app.core.ui.UiState
import jp.plesion.app.data.model.Comment
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.repository.PostRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PostDetail(val post: Post, val comments: List<Comment>)

class PostDetailViewModel(
    private val postId: String,
    private val postRepository: PostRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<PostDetail>>(UiState.Loading)
    val uiState: StateFlow<UiState<PostDetail>> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.value = UiState.Loading
            val comments = async { postRepository.getComments(postId) }
            val post = postRepository.getPost(postId).getOrElse { e ->
                comments.cancel()
                _uiState.value = UiState.Error(
                    if (e is NoSuchElementException) "投稿が見つかりません。削除された可能性があります" else "投稿を読み込めませんでした",
                )
                return@launch
            }
            _uiState.value = comments.await().fold(
                onSuccess = { UiState.Success(PostDetail(post, it)) },
                onFailure = { UiState.Error("コメントを読み込めませんでした") },
            )
        }
    }
}
