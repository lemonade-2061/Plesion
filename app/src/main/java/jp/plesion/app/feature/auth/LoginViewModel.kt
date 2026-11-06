package jp.plesion.app.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.plesion.app.data.repository.AuthRepository
import jp.plesion.app.data.repository.GoogleSignInCancelledException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class LoginUiState(
    val isLoading: Boolean = false,
    val isSignedIn: Boolean = false,
    val errorMessage: String? = null,
)

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        // ログイン済みでアプリを開き直したときは、ログイン画面を飛ばす
        viewModelScope.launch {
            authRepository.currentUser.filterNotNull().first()
            _uiState.value = LoginUiState(isSignedIn = true)
        }
    }

    fun signIn(getIdToken: suspend () -> Result<String>) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            getIdToken()
                .fold(
                    onSuccess = { authRepository.signInWithGoogle(it) },
                    onFailure = { Result.failure(it) },
                )
                .onSuccess { _uiState.value = LoginUiState(isSignedIn = true) }
                .onFailure { e ->
                    val message = if (e is GoogleSignInCancelledException) null else e.message
                    _uiState.value = LoginUiState(errorMessage = message)
                }
        }
    }
}