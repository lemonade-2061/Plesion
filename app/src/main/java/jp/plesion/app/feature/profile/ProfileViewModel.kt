package jp.plesion.app.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import jp.plesion.app.core.ui.UiState
import jp.plesion.app.core.ui.toUiState
import jp.plesion.app.data.local.SampleData
import jp.plesion.app.data.model.User
import jp.plesion.app.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val userRepository: UserRepository) : ViewModel() {
    private val _uiState = MutableStateFlow<UiState<User>>(UiState.Loading)
    val uiState: StateFlow<UiState<User>> = _uiState.asStateFlow()

    init {
        load()
    }

    // TODO(tajima): AuthRepository.currentUser から自分の ID を取るようにする
    fun load() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = userRepository.getUser(SampleData.me.id).toUiState()
        }
    }
}
