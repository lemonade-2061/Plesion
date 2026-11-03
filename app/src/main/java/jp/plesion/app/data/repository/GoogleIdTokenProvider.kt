package jp.plesion.app.data.repository

import android.content.Context

/** Google のアカウント選択を表示して、ログイン用の ID トークンを取得する窓口。 */
interface GoogleIdTokenProvider {
    /** 取り消されたら Result.failure(GoogleSignInCancelledException) を返す。[activityContext] には Activity を渡す。 */
    suspend fun getIdToken(activityContext: Context): Result<String>
}

/** ユーザーがアカウント選択を閉じた（エラーではない）。 */
class GoogleSignInCancelledException : Exception("Google sign-in was cancelled")
