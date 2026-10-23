package jp.plesion.app.core.ui

/**
 * 画面の状態を「読み込み中 / 成功 / 失敗」の3つで表す共通の型。
 * ViewModel は StateFlow<UiState<T>> を公開し、画面は UiStateContent で描き分ける。
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun <T> Result<T>.toUiState(): UiState<T> = fold(
    onSuccess = { UiState.Success(it) },
    onFailure = { UiState.Error(it.message ?: "不明なエラーが発生しました") },
)
