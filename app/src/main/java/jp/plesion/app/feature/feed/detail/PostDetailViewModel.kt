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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PostDetail(val post: Post, val comments: List<Comment>)

/** 自分の投稿のときの編集・削除の状態。 */
data class PostActionState(
    /** 自分の投稿か。編集・削除ボタンを出すかに使う（匿名投稿も判定できる）。 */
    val isMine: Boolean = false,
    /** 編集中の本文。編集していなければ null。 */
    val editingText: String? = null,
    val isWorking: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null,
)

/** コメント入力欄の状態。投稿の読み込み状態（uiState）とは別に持つ。 */
data class CommentInputState(
    val text: String = "",
    val isSending: Boolean = false,
    val errorMessage: String? = null,
)

class PostDetailViewModel(
    private val postId: String,
    private val postRepository: PostRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<PostDetail>>(UiState.Loading)
    val uiState: StateFlow<UiState<PostDetail>> = _uiState.asStateFlow()

    private val _postAction = MutableStateFlow(PostActionState())
    val postAction: StateFlow<PostActionState> = _postAction.asStateFlow()

    private val _commentInput = MutableStateFlow(CommentInputState())
    val commentInput: StateFlow<CommentInputState> = _commentInput.asStateFlow()

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
            // 判定に失敗したときは、編集・削除ボタンを出さないだけにする
            val isMine = postRepository.isMyPost(post).getOrDefault(false)
            _postAction.update { it.copy(isMine = isMine) }
        }
    }

    fun onCommentTextChange(text: String) {
        _commentInput.update { it.copy(text = text.take(Post.MAX_TEXT_LENGTH), errorMessage = null) }
    }

    fun sendComment() {
        val input = _commentInput.value
        // 連打で二重に送らないよう、送信中は受け付けない
        if (input.isSending || input.text.isBlank()) return
        _commentInput.update { it.copy(isSending = true, errorMessage = null) }
        viewModelScope.launch {
            postRepository.addComment(postId, input.text)
                .onSuccess { comment ->
                    // 読み直さずに、表示中の一覧の末尾に足す（コメントは古い順）
                    _uiState.update { state ->
                        if (state !is UiState.Success) return@update state
                        val detail = state.data
                        UiState.Success(
                            detail.copy(
                                post = detail.post.copy(commentCount = detail.post.commentCount + 1),
                                comments = detail.comments + comment,
                            ),
                        )
                    }
                    _commentInput.value = CommentInputState()
                }
                .onFailure {
                    _commentInput.update { it.copy(isSending = false, errorMessage = "コメントを送信できませんでした") }
                }
        }
    }

    fun startEdit() {
        val post = (_uiState.value as? UiState.Success)?.data?.post ?: return
        _postAction.update { it.copy(editingText = post.text, errorMessage = null) }
    }

    fun onEditTextChange(text: String) {
        _postAction.update { it.copy(editingText = text.take(Post.MAX_TEXT_LENGTH), errorMessage = null) }
    }

    fun cancelEdit() {
        _postAction.update { it.copy(editingText = null, errorMessage = null) }
    }

    fun saveEdit() {
        val action = _postAction.value
        val text = action.editingText ?: return
        if (action.isWorking || text.isBlank()) return
        _postAction.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            postRepository.updatePost(postId, text)
                .onSuccess { updated ->
                    _uiState.update { state ->
                        if (state is UiState.Success) UiState.Success(state.data.copy(post = updated)) else state
                    }
                    _postAction.update { it.copy(editingText = null, isWorking = false) }
                }
                .onFailure {
                    _postAction.update { it.copy(isWorking = false, errorMessage = "投稿を編集できませんでした") }
                }
        }
    }

    fun deletePost() {
        if (_postAction.value.isWorking) return
        _postAction.update { it.copy(isWorking = true, errorMessage = null) }
        viewModelScope.launch {
            postRepository.deletePost(postId)
                .onSuccess { _postAction.update { it.copy(isWorking = false, isDeleted = true) } }
                .onFailure {
                    _postAction.update { it.copy(isWorking = false, errorMessage = "投稿を削除できませんでした") }
                }
        }
    }
}
