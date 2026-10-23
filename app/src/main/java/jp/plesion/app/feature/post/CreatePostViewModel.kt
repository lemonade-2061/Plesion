package jp.plesion.app.feature.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.plesion.app.data.repository.PostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CreatePostUiState(
    val text: String = "",
    val isSubmitting: Boolean = false,
    val isPosted: Boolean = false,
    val errorMessage: String? = null,
)

class CreatePostViewModel(private val postRepository: PostRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState: StateFlow<CreatePostUiState> = _uiState.asStateFlow()

    fun onTextChange(text: String) {
        _uiState.update { it.copy(text = text, errorMessage = null) }
    }

    // TODO(tajima): 画像添付・位置情報の付与を追加する
    fun submit() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSubmitting = true, errorMessage = null) }
            postRepository.createPost(_uiState.value.text, location = null)
                .onSuccess { _uiState.update { it.copy(isSubmitting = false, isPosted = true) } }
                .onFailure { e ->
                    _uiState.update { it.copy(isSubmitting = false, errorMessage = e.message) }
                }
        }
    }
}
