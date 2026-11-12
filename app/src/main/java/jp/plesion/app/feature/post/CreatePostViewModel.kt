package jp.plesion.app.feature.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.plesion.app.core.location.LocationProvider
import jp.plesion.app.core.location.toLocationErrorMessage
import jp.plesion.app.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 投稿本文の最大文字数。 */
const val MAX_POST_LENGTH = 500

data class CreatePostUiState(
    val text: String = "",
    val isAnonymous: Boolean = false,
    val isSubmitting: Boolean = false,
    val isLocating: Boolean = false,
    val isPosted: Boolean = false,
    val errorMessage: String? = null,
)

class CreatePostViewModel(
    private val postRepository: PostRepository,
    private val locationProvider: LocationProvider,
) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun onTextChange(text: String) {
        // 貼り付けなどで上限を超えても、300文字までで切る
        _uiState.update { it.copy(text = text.take(MAX_POST_LENGTH), errorMessage = null) }
    }

    fun onAnonymousChange(isAnonymous: Boolean) {
        _uiState.update { it.copy(isAnonymous = isAnonymous) }
    }

    // TODO(tajima): 画像添付を追加する
    fun submit() {
        // 連打で二重投稿しないよう、送信中は受け付けない
        if (_uiState.value.isSubmitting) return
        _uiState.update { it.copy(isSubmitting = true, isLocating = true, errorMessage = null) }
        viewModelScope.launch {
            // 位置の丸め・geohash は Repository の責務なので、ここでは取得した現在地をそのまま渡す
            val location = locationProvider.getCurrentLocation().getOrElse { e ->
                _uiState.update {
                    it.copy(isSubmitting = false, isLocating = false, errorMessage = e.toLocationErrorMessage())
                }
                return@launch
            }
            _uiState.update { it.copy(isLocating = false) }
            val state = _uiState.value
            postRepository.createPost(state.text, location, isAnonymous = state.isAnonymous)
                .onSuccess { _uiState.update { it.copy(isSubmitting = false, isPosted = true) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = e.message) }
                }
        }
    }

    /** 投稿後にホームへ戻った後、タブで再び開いたときに前の入力や完了状態が残らないようにする。 */
    fun onPostedHandled() {
        _uiState.value = CreatePostUiState()
    }
}