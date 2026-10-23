package jp.plesion.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.plesion.app.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val errorMessage: String? = null,
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // TODO(tajima/takuma): Credential Manager で取得した Google の ID トークンを渡す
    fun signInWithSampleUser() {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            authRepository.signInWithGoogle(idToken = "sample-token")
                .onSuccess { _uiState.value = LoginUiState(isSignedIn = true) }
                .onFailure { _uiState.value = LoginUiState(errorMessage = it.message) }
        }
    }
}
