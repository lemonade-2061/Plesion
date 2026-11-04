package jp.plesion.app.di

import android.annotation.SuppressLint
import android.content.Context
import jp.plesion.app.core.location.FusedLocationProvider
import jp.plesion.app.core.location.LocationProvider
import jp.plesion.app.data.local.FakeAuthRepository
import jp.plesion.app.data.local.FakeGoogleIdTokenProvider
import jp.plesion.app.data.local.FakePostRepository
import jp.plesion.app.data.local.FakeUserRepository
import jp.plesion.app.data.remote.CredentialManagerGoogleIdTokenProvider
import jp.plesion.app.data.remote.FirebaseAuthRepository
import jp.plesion.app.data.remote.FirestorePostRepository
import jp.plesion.app.data.remote.FirestoreUserRepository
import jp.plesion.app.data.repository.AuthRepository
import jp.plesion.app.data.repository.GoogleIdTokenProvider
import jp.plesion.app.data.repository.PostRepository
import jp.plesion.app.data.repository.UserRepository

/**
 * 手動 DI のコンテナ。画面は具体クラスではなく Repository インターフェースだけを知る。
 *
 * google-services.json があれば Firebase の本番実装、なければ Fake（仮データ）を使う。
 * 画面側のコードはどちらでも変わらない。
 */
class AppContainer(context: Context) {
    // google-services.json がない環境（CI など）では Fake のまま動かす
    private val webClientId: String? = context.googleWebClientId()
    private val useFirebase = webClientId != null

    val authRepository: AuthRepository =
        if (useFirebase) FirebaseAuthRepository() else FakeAuthRepository()
    val googleIdTokenProvider: GoogleIdTokenProvider =
        if (webClientId != null) CredentialManagerGoogleIdTokenProvider(webClientId) else FakeGoogleIdTokenProvider()
    val userRepository: UserRepository =
        if (useFirebase) FirestoreUserRepository() else FakeUserRepository()
    val postRepository: PostRepository =
        if (useFirebase) FirestorePostRepository() else FakePostRepository()

    /** 権限がなければ Result.failure を返す。権限リクエストは各画面で行う。 */
    val locationProvider: LocationProvider = FusedLocationProvider(context.applicationContext)
}

/**
 * google-services プラグインが生成する Web クライアント ID。なければ null。
 * json がないとリソース自体が生成されないので、R.string では参照できない（CI のビルドが通らなくなる）。
 */
@SuppressLint("DiscouragedApi")
private fun Context.googleWebClientId(): String? {
    val id = resources.getIdentifier("default_web_client_id", "string", packageName)
    return if (id != 0) getString(id) else null
}
