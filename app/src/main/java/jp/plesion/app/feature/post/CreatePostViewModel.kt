package jp.plesion.app.feature.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.plesion.app.core.location.LocationProvider
import jp.plesion.app.core.location.toLocationErrorMessage
import jp.plesion.app.data.model.Post
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
    /** Photo Picker で選んだ画像。縮小・アップロードは PostRepository が行う。 */
    val imageUris: List<Uri> = emptyList(),
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

    /** 上限（[Post.MAX_IMAGES] 枚）を超えた分は捨てる。 */
    fun onImagesPicked(uris: List<Uri>) {
        _uiState.update { state ->
            state.copy(imageUris = (state.imageUris + uris).distinct().take(Post.MAX_IMAGES), errorMessage = null)
        }
    }

    fun onImageRemoved(uri: Uri) {
        _uiState.update { it.copy(imageUris = it.imageUris - uri) }
    }

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
            postRepository.createPost(state.text, location, isAnonymous = state.isAnonymous, imageUris = state.imageUris)
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