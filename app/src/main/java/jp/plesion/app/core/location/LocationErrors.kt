package jp.plesion.app.core.location

/** LocationProvider の失敗を画面に出す文言に変える。ホームと投稿作成で同じ文言にするため共通化。 */
fun Throwable.toLocationErrorMessage(): String = when (this) {
    is LocationPermissionDeniedException -> "正確な位置情報の許可がないため、現在地を取得できません"
    is LocationUnavailableException -> "現在地を取得できませんでした。端末の位置情報がONか確認して、もう一度お試しください"
    else -> "現在地の取得に失敗しました。もう一度お試しください"
}
