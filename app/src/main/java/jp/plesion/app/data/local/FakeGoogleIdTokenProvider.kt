package jp.plesion.app.data.local

import android.content.Context
import jp.plesion.app.data.repository.GoogleIdTokenProvider

/** 【仮実装】アカウント選択を出さずに仮のトークンを返す。FakeAuthRepository と組み合わせて使う。 */
class FakeGoogleIdTokenProvider : GoogleIdTokenProvider {
    override suspend fun getIdToken(activityContext: Context): Result<String> =
        Result.success("sample-token")
}
