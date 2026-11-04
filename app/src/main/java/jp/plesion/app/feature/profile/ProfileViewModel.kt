package jp.plesion.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.plesion.app.core.ui.UiState
import jp.plesion.app.core.ui.toUiState
import jp.plesion.app.data.model.User
import jp.plesion.app.data.repository.AuthRepository
import jp.plesion.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val userRepository: UserRepository,
    private val authRepository: AuthRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<User>>(UiState.Loading)
    val uiState: StateFlow<UiState<User>> = _uiState.asStateFlow()

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
    }
}
