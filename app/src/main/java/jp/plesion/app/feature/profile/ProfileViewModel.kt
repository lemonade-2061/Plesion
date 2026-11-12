package jp.plesion.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.plesion.app.core.ui.UiState
import jp.plesion.app.core.ui.toUiState
import jp.plesion.app.feature.feed.formatTimeAgo
import jp.plesion.app.data.model.User
import jp.plesion.app.data.model.Post
import jp.plesion.app.data.repository.AuthRepository
import jp.plesion.app.data.repository.PostRepository
import jp.plesion.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
    private val postRepository: PostRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<User>>(UiState.Loading)
    val uiState: StateFlow<UiState<User>> = _uiState.asStateFlow()

    /** 自分の投稿。読み込み中・失敗のときは null（「自分の投稿」の欄ごと出さない）。 */
    private val _posts = MutableStateFlow<List<ProfilePostUi>?>(null)
    val posts: StateFlow<List<ProfilePostUi>?> = _posts.asStateFlow()

    init {
        load()
    }

    /** ログイン中のユーザー自身のプロフィールを読み込む。 */
    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val me = authRepository.currentUser.first()
            _uiState.value = if (me == null) {
                UiState.Error("ログインしていません")
            } else {
                userRepository.getUser(me.id).toUiState()
            }
        }
        loadPosts()
    }

    /** 投稿の編集・削除の後など、自分の投稿だけ読み直す。 */
    fun loadPosts() {
        viewModelScope.launch {
            _posts.value = postRepository.getMyPosts().getOrNull()?.map { it.toProfilePostUi() }
        }
    }
}

private fun Post.toProfilePostUi(): ProfilePostUi = ProfilePostUi(
    id = id,
    body = text,
    timeAgo = formatTimeAgo(createdAt),
    commentCount = commentCount,
    isAnonymous = isAnonymous,
)
